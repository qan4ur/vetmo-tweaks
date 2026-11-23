/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.hud.XAnchor;
import meteordevelopment.meteorclient.systems.hud.YAnchor;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.visuals.hud.VetmoHudModule;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix3x2fStack;

public class ComboCounter extends Module implements VetmoHudModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgHud = settings.createGroup("HUD");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Boolean> playersOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("players-only")
        .description("Only count hits on players.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> timeout = sgGeneral.add(new DoubleSetting.Builder()
        .name("timeout")
        .description("Seconds without landing a hit before the combo resets.")
        .defaultValue(2.5)
        .range(0.5, 10)
        .sliderRange(0.5, 6)
        .build()
    );

    private final Setting<Boolean> resetOnHurt = sgGeneral.add(new BoolSetting.Builder()
        .name("reset-on-hurt")
        .description("Reset the combo when you take damage.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> minCombo = sgGeneral.add(new IntSetting.Builder()
        .name("min-combo")
        .description("Only show the counter from this many hits.")
        .defaultValue(2)
        .range(1, 10)
        .sliderRange(1, 10)
        .build()
    );

    private final Setting<String> label = sgGeneral.add(new StringSetting.Builder()
        .name("label")
        .description("Text shown after the number.")
        .defaultValue("COMBO")
        .build()
    );

    private final Setting<Boolean> background = sgGeneral.add(new BoolSetting.Builder()
        .name("background")
        .description("Draws a panel behind the counter.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> popStrength = sgGeneral.add(new DoubleSetting.Builder()
        .name("pop-strength")
        .description("How much the number grows when the combo goes up.")
        .defaultValue(0.45)
        .range(0, 1.5)
        .sliderRange(0, 1)
        .build()
    );

    private final Setting<Boolean> previewInChat = sgGeneral.add(new BoolSetting.Builder()
        .name("preview-in-chat")
        .description("Shows a preview while chat is open.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showInScreens = sgGeneral.add(new BoolSetting.Builder()
        .name("show-in-screens")
        .description("Keep rendering while a non-chat screen is open.")
        .defaultValue(false)
        .build()
    );

    // HUD

    private final Setting<Double> scale = sgHud.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Size of the counter. Mouse wheel over it in the HUD editor also changes it.")
        .defaultValue(1)
        .range(0.3, 4)
        .sliderRange(0.5, 3)
        .build()
    );

    // Colors

    private final Setting<SettingColor> numberColor = sgColors.add(new ColorSetting.Builder()
        .name("number")
        .description("Color of the combo number.")
        .defaultValue(new SettingColor(220, 20, 40))
        .build()
    );

    private final Setting<SettingColor> flashColor = sgColors.add(new ColorSetting.Builder()
        .name("flash")
        .description("Color the number flashes to on a new hit.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> labelColor = sgColors.add(new ColorSetting.Builder()
        .name("label")
        .description("Color of the label text.")
        .defaultValue(new SettingColor(255, 170, 170))
        .build()
    );

    private final Setting<SettingColor> backgroundColor = sgColors.add(new ColorSetting.Builder()
        .name("background")
        .description("Panel background.")
        .defaultValue(new SettingColor(18, 6, 8, 190))
        .visible(background::get)
        .build()
    );

    private final Setting<SettingColor> outlineColor = sgColors.add(new ColorSetting.Builder()
        .name("outline")
        .description("Panel outline.")
        .defaultValue(new SettingColor(220, 20, 40, 110))
        .visible(background::get)
        .build()
    );

    private final Setting<SettingColor> glowColor = sgColors.add(new ColorSetting.Builder()
        .name("glow")
        .description("Glow along the top edge of the panel.")
        .defaultValue(new SettingColor(255, 40, 40, 120))
        .visible(background::get)
        .build()
    );

    private final Setting<SettingColor> shadowColor = sgColors.add(new ColorSetting.Builder()
        .name("shadow")
        .description("Soft drop shadow.")
        .defaultValue(new SettingColor(0, 0, 0, 90))
        .visible(background::get)
        .build()
    );

    // State

    private LivingEntity pending;
    private int pendingTicks;
    private int pendingLastHurt;
    private int lastPlayerHurt;

    private int combo;
    private int displayCombo;
    private long lastHit;

    private double pop;
    private double fade;
    private double shake;

    public ComboCounter() {
        super(Categories.Visuals, "combo-counter", "Counts your consecutive landed hits.");
    }

    @Override
    public void onActivate() {
        pending = null;
        combo = 0;
        displayCombo = 0;
        fade = 0;
        pop = 0;
        lastPlayerHurt = mc.player != null ? mc.player.hurtTime : 0;
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        onActivate();
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (!(event.entity instanceof LivingEntity living) || !living.isAlive()) return;
        if (playersOnly.get() && !(living instanceof PlayerEntity)) return;
        pending = living;
        pendingTicks = 0;
        pendingLastHurt = living.hurtTime;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        // Taking damage restarts hurtTime at its max: an increase means we just got hit.
        int ph = mc.player.hurtTime;
        if (resetOnHurt.get() && ph > lastPlayerHurt) reset();
        lastPlayerHurt = ph;

        // A hit "lands" when the attacked entity's hurt animation restarts shortly after we swung.
        if (pending != null) {
            pendingTicks++;
            if (pending.isRemoved()) {
                pending = null;
            } else if (pending.hurtTime > pendingLastHurt) {
                hit();
                pending = null;
            } else {
                pendingLastHurt = pending.hurtTime;
                if (pendingTicks > 8) pending = null;
            }
        }

        if (combo > 0 && System.currentTimeMillis() - lastHit > timeout.get() * 1000) reset();
    }

    private void hit() {
        combo++;
        displayCombo = combo;
        lastHit = System.currentTimeMillis();
        pop = 1;
    }

    private void reset() {
        if (combo >= minCombo.get()) shake = 1;
        combo = 0;
    }

    // ---- animation (self-timed, the HUD element drives it every frame) ----

    private long lastFrame;
    private boolean active;

    private void update() {
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : VetmoRender.dt((now - lastFrame) / 1e9);
        lastFrame = now;
        if (mc.player == null) return;

        active = combo >= minCombo.get();
        boolean preview = !active && previewInChat.get() && VetmoRender.isChatOpen();
        if (preview) displayCombo = Math.max(displayCombo, 3);

        fade = VetmoRender.approach(fade, active || preview ? 1 : 0, active ? 14 : 6, dt);
        pop = Math.max(0, pop - dt * 3.5);
        shake = Math.max(0, shake - dt * 2.5);
        if (fade < 0.004) {
            fade = 0;
            if (!active) displayCombo = 0;
        }
    }

    // ---- HUD element ----

    private static final float NUM_SCALE = 2f;
    private static final int PANEL_PAD = 7;
    private static final int HALF_H = 13;

    private int shownCombo() {
        return displayCombo > 0 ? displayCombo : 5;
    }

    private float contentWidth(int number) {
        TextRenderer tr = mc.textRenderer;
        String lbl = label.get();
        int lblW = lbl.isEmpty() ? 0 : tr.getWidth(lbl);
        return tr.getWidth(Integer.toString(number)) * NUM_SCALE + (lblW > 0 ? 4 + lblW : 0);
    }

    @Override
    public double getHudWidth() {
        return Math.ceil(contentWidth(shownCombo()) / 2) * 2 + PANEL_PAD * 2;
    }

    @Override
    public double getHudHeight() {
        return HALF_H * 2;
    }

    @Override
    public boolean shouldRenderHud() {
        update();
        return fade > 0 && VetmoRender.shouldRender(showInScreens.get());
    }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, boolean inEditor) {
        update();
        boolean preview = inEditor && fade <= 0;
        if (!preview && fade <= 0) return;

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate((float) (getHudWidth() / 2), HALF_H);
        if (preview) render(ctx, shownCombo(), true, 1, 1);
        else {
            double eased = VetmoRender.easeOutCubic(fade);
            render(ctx, displayCombo, active, fade, active ? 0.85 + 0.15 * eased : 0.95 + 0.05 * eased);
        }
        m.popMatrix();
    }

    @Override
    public Setting<Double> hudScale() {
        return scale;
    }

    @Override
    public int defaultHudX() {
        return 0;
    }

    @Override
    public int defaultHudY() {
        return (int) Math.round(-40 * mc.getWindow().getScaleFactor());
    }

    @Override
    public XAnchor defaultHudXAnchor() {
        return XAnchor.Center;
    }

    @Override
    public YAnchor defaultHudYAnchor() {
        return YAnchor.Center;
    }

    /** Draws the counter centered on (0, 0). */
    private void render(DrawContext ctx, int number, boolean active, double fade, double s) {
        TextRenderer tr = mc.textRenderer;

        String num = Integer.toString(number);
        String lbl = label.get();
        final float numScale = NUM_SCALE;

        int numW = tr.getWidth(num);
        int lblW = lbl.isEmpty() ? 0 : tr.getWidth(lbl);
        float totalW = numW * numScale + (lblW > 0 ? 4 + lblW : 0);

        // Little shake when a combo breaks
        float shakeX = (float) (Math.sin(System.currentTimeMillis() / 18.0) * 3 * shake * shake);

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(shakeX, 0);
        m.scale((float) s, (float) s);

        double a = fade;
        int left = (int) Math.floor(-totalW / 2);

        if (background.get()) {
            int x1 = left - PANEL_PAD, x2 = (int) Math.ceil(totalW / 2) + PANEL_PAD;
            VetmoRender.panel(ctx, x1, -HALF_H, x2, HALF_H, 4,
                VetmoRender.withAlpha(backgroundColor.get().getPacked(), a),
                VetmoRender.withAlpha(outlineColor.get().getPacked(), a),
                VetmoRender.withAlpha(shadowColor.get().getPacked(), a), 4,
                VetmoRender.withAlpha(glowColor.get().getPacked(), a * (0.6 + 0.4 * pop)));
        }

        // Number with pop scale around its own center
        double p = pop * pop;
        float popScale = (float) (1 + popStrength.get() * p);
        float numCx = left + numW * numScale / 2f;
        int numColor = VetmoRender.lerpColor(p, numberColor.get().getPacked(), flashColor.get().getPacked());
        if (!active) numColor = VetmoRender.lerpColor(shake, numColor, 0xFF808080);

        m.pushMatrix();
        m.translate(numCx, 0);
        m.scale(numScale * popScale, numScale * popScale);
        VetmoRender.text(ctx, num, -numW / 2, -4, VetmoRender.withAlpha(numColor, a), true);
        m.popMatrix();

        if (lblW > 0) {
            int lx = Math.round(left + numW * numScale + 4);
            VetmoRender.text(ctx, lbl, lx, 1, VetmoRender.withAlpha(labelColor.get().getPacked(), a), true);
        }

        m.popMatrix();
    }
}
