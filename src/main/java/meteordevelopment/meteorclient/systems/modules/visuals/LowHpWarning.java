/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

public class LowHpWarning extends Module {
    public enum PulseMode {
        Sine,
        Heartbeat,
        Static
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgStyle = settings.createGroup("Style");

    // General

    private final Setting<Double> threshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("threshold")
        .description("Health (in half-hearts) below which the warning shows. 8 = 4 hearts.")
        .defaultValue(8)
        .range(1, 40)
        .sliderRange(1, 20)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Boolean> includeAbsorption = sgGeneral.add(new BoolSetting.Builder()
        .name("include-absorption")
        .description("Count absorption hearts as health.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> scaleWithHealth = sgGeneral.add(new BoolSetting.Builder()
        .name("scale-with-health")
        .description("The lower your health, the stronger the vignette.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreCreative = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-creative")
        .description("Don't show in creative or spectator mode.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showInScreens = sgGeneral.add(new BoolSetting.Builder()
        .name("show-in-screens")
        .description("Keep rendering while a non-chat screen is open.")
        .defaultValue(false)
        .build()
    );

    // Style

    private final Setting<PulseMode> pulseMode = sgStyle.add(new EnumSetting.Builder<PulseMode>()
        .name("pulse-mode")
        .description("How the vignette pulses.")
        .defaultValue(PulseMode.Heartbeat)
        .build()
    );

    private final Setting<Double> pulseSpeed = sgStyle.add(new DoubleSetting.Builder()
        .name("pulse-speed")
        .description("Pulses per second.")
        .defaultValue(1.2)
        .range(0.1, 5)
        .sliderRange(0.2, 3)
        .visible(() -> pulseMode.get() != PulseMode.Static)
        .build()
    );

    private final Setting<Boolean> fasterWhenLower = sgStyle.add(new BoolSetting.Builder()
        .name("faster-when-lower")
        .description("Pulse faster as health drops.")
        .defaultValue(true)
        .visible(() -> pulseMode.get() != PulseMode.Static)
        .build()
    );

    private final Setting<Double> minPulse = sgStyle.add(new DoubleSetting.Builder()
        .name("min-pulse")
        .description("Lowest intensity between pulses (0-1).")
        .defaultValue(0.35)
        .range(0, 1)
        .sliderRange(0, 1)
        .visible(() -> pulseMode.get() != PulseMode.Static)
        .build()
    );

    private final Setting<Integer> maxAlpha = sgStyle.add(new IntSetting.Builder()
        .name("max-alpha")
        .description("Maximum opacity at the very edge of the screen.")
        .defaultValue(160)
        .range(0, 255)
        .sliderRange(0, 255)
        .build()
    );

    private final Setting<Integer> thickness = sgStyle.add(new IntSetting.Builder()
        .name("thickness")
        .description("How far the vignette reaches into the screen (GUI pixels).")
        .defaultValue(70)
        .range(5, 400)
        .sliderRange(10, 200)
        .build()
    );

    private final Setting<SettingColor> color = sgStyle.add(new ColorSetting.Builder()
        .name("color")
        .description("Vignette color (alpha is controlled by max-alpha).")
        .defaultValue(new SettingColor(220, 20, 40))
        .build()
    );

    private final Setting<Boolean> screenTint = sgStyle.add(new BoolSetting.Builder()
        .name("screen-tint")
        .description("Also tints the whole screen slightly during pulses.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> tintAlpha = sgStyle.add(new IntSetting.Builder()
        .name("tint-alpha")
        .description("Maximum opacity of the screen tint.")
        .defaultValue(25)
        .range(0, 120)
        .sliderRange(0, 80)
        .visible(screenTint::get)
        .build()
    );

    // Number of linear gradient segments used to approximate the eased falloff.
    private static final int SEGMENTS = 8;

    private double fade;
    private double phase;

    public LowHpWarning() {
        super(Categories.Visuals, "Low HP Warning", "Pulses a red vignette at the screen edges when your health is low.");
    }

    @Override
    public void onActivate() {
        fade = 0;
        phase = 0;
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null) return;
        double dt = VetmoRender.dt(event.frameTime);

        float hp = mc.player.getHealth();
        if (includeAbsorption.get()) hp += mc.player.getAbsorptionAmount();
        double limit = threshold.get();

        boolean excluded = ignoreCreative.get() && (mc.player.isCreative() || mc.player.isSpectator());
        boolean low = !excluded && mc.player.isAlive() && hp < limit;

        // severity 0..1: 0 at threshold, 1 at zero health
        double severity = VetmoRender.clamp01(1 - hp / limit);

        fade = VetmoRender.approach(fade, low ? 1 : 0, low ? 6 : 3, dt);

        double speed = pulseSpeed.get() * (fasterWhenLower.get() ? 1 + severity : 1);
        phase = (phase + dt * speed) % 1.0;

        if (fade < 0.004) {
            fade = 0;
            return;
        }
        if (!VetmoRender.shouldRender(showInScreens.get())) return;

        double pulse = switch (pulseMode.get()) {
            case Sine -> 0.5 - 0.5 * Math.cos(phase * Math.PI * 2);
            case Heartbeat -> heartbeat(phase);
            case Static -> 1;
        };
        double min = pulseMode.get() == PulseMode.Static ? 1 : minPulse.get();
        double intensity = min + (1 - min) * pulse;
        if (scaleWithHealth.get()) intensity *= 0.45 + 0.55 * severity;
        intensity *= fade;

        int alpha = (int) Math.round(maxAlpha.get() * VetmoRender.clamp01(intensity));
        if (alpha <= 0) return;

        int base = color.get().getPacked() & 0x00FFFFFF;
        DrawContext ctx = event.drawContext;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        int t = Math.min(thickness.get(), Math.min(w, h) / 2);

        if (screenTint.get()) {
            int ta = (int) Math.round(tintAlpha.get() * VetmoRender.clamp01(pulse * fade * (0.5 + 0.5 * severity)));
            if (ta > 0) ctx.fill(0, 0, w, h, (ta << 24) | base);
        }

        Matrix3x2fStack m = ctx.getMatrices();

        // Only rotations are used (no mirroring) so quad winding is preserved.
        // Top: opaque at y = 0 fading downwards
        edge(ctx, w, t, base, alpha);

        // Bottom: rotate 180 degrees around the screen. (u, v) -> (w - u, h - v)
        m.pushMatrix();
        m.translate(w, h);
        m.rotate((float) Math.PI);
        edge(ctx, w, t, base, alpha);
        m.popMatrix();

        // Left: (u, v) -> (v, h - u)
        m.pushMatrix();
        m.translate(0, h);
        m.rotate((float) (-Math.PI / 2));
        edge(ctx, h, t, base, alpha);
        m.popMatrix();

        // Right: (u, v) -> (w - v, u)
        m.pushMatrix();
        m.translate(w, 0);
        m.rotate((float) (Math.PI / 2));
        edge(ctx, h, t, base, alpha);
        m.popMatrix();
    }

    /**
     * Draws a band from (0, 0) to (len, depth) in local space, opaque at local y = 0 fading to clear at y = depth,
     * using an eased (quadratic) falloff built from several linear gradient segments so there's no visible banding.
     */
    private void edge(DrawContext ctx, int len, int depth, int rgb, int alpha) {
        int prevY = 0;
        int prevA = alpha;
        for (int i = 1; i <= SEGMENTS; i++) {
            double f = (double) i / SEGMENTS;
            int segY = (int) Math.round(depth * f);
            double falloff = (1 - f) * (1 - f);
            int segA = (int) Math.round(alpha * falloff);
            if (segY > prevY) {
                ctx.fillGradient(0, prevY, len, segY, (prevA << 24) | rgb, (segA << 24) | rgb);
            }
            prevY = segY;
            prevA = segA;
        }
    }

    /** Two quick thumps followed by a rest, in 0..1. */
    private static double heartbeat(double p) {
        double a = Math.exp(-Math.pow((p - 0.08) / 0.045, 2));
        double b = 0.7 * Math.exp(-Math.pow((p - 0.27) / 0.05, 2));
        return Math.min(1, a + b);
    }
}
