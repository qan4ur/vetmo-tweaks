/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.events.meteor.MouseButtonEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public class Keystrokes extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPosition = settings.createGroup("Position");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Boolean> showMovement = sgGeneral.add(new BoolSetting.Builder()
        .name("movement-keys")
        .description("Shows the W A S D keys.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showMouse = sgGeneral.add(new BoolSetting.Builder()
        .name("mouse-buttons")
        .description("Shows the left and right mouse buttons.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showCps = sgGeneral.add(new BoolSetting.Builder()
        .name("cps")
        .description("Shows clicks per second under the mouse buttons.")
        .defaultValue(true)
        .visible(showMouse::get)
        .build()
    );

    private final Setting<Boolean> showSpace = sgGeneral.add(new BoolSetting.Builder()
        .name("space-bar")
        .description("Shows the jump key.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showSneak = sgGeneral.add(new BoolSetting.Builder()
        .name("sneak-key")
        .description("Shows the sneak key next to the space bar.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> boundNames = sgGeneral.add(new BoolSetting.Builder()
        .name("bound-key-names")
        .description("Label keys with what they are actually bound to instead of W A S D.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> keySize = sgGeneral.add(new IntSetting.Builder()
        .name("key-size")
        .description("Size of one key box.")
        .defaultValue(22)
        .range(14, 40)
        .sliderRange(14, 34)
        .build()
    );

    private final Setting<Integer> gap = sgGeneral.add(new IntSetting.Builder()
        .name("gap")
        .description("Space between keys.")
        .defaultValue(2)
        .range(0, 8)
        .sliderRange(0, 6)
        .build()
    );

    private final Setting<Double> fadeSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("fade-speed")
        .description("How fast keys fade back after being released.")
        .defaultValue(8)
        .range(1, 40)
        .sliderRange(2, 25)
        .build()
    );

    private final Setting<Boolean> showInScreens = sgGeneral.add(new BoolSetting.Builder()
        .name("show-in-screens")
        .description("Keep rendering while a non-chat screen is open.")
        .defaultValue(false)
        .build()
    );

    // Position

    private final Setting<Double> posX = sgPosition.add(new DoubleSetting.Builder()
        .name("x")
        .description("Horizontal position in percent of the screen width.")
        .defaultValue(1)
        .range(0, 100)
        .sliderRange(0, 100)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Double> posY = sgPosition.add(new DoubleSetting.Builder()
        .name("y")
        .description("Vertical position in percent of the screen height.")
        .defaultValue(40)
        .range(0, 100)
        .sliderRange(0, 100)
        .decimalPlaces(1)
        .build()
    );

    private final Setting<Double> scale = sgPosition.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Overall size.")
        .defaultValue(1)
        .range(0.5, 3)
        .sliderRange(0.5, 2)
        .build()
    );

    // Colors

    private final Setting<SettingColor> keyColor = sgColors.add(new ColorSetting.Builder()
        .name("key")
        .description("Background of a released key.")
        .defaultValue(new SettingColor(18, 6, 8, 190))
        .build()
    );

    private final Setting<SettingColor> pressedColor = sgColors.add(new ColorSetting.Builder()
        .name("pressed")
        .description("Background of a pressed key.")
        .defaultValue(new SettingColor(220, 20, 40, 230))
        .build()
    );

    private final Setting<SettingColor> outlineColor = sgColors.add(new ColorSetting.Builder()
        .name("outline")
        .description("Key outline.")
        .defaultValue(new SettingColor(130, 0, 15, 160))
        .build()
    );

    private final Setting<SettingColor> glowColor = sgColors.add(new ColorSetting.Builder()
        .name("glow")
        .description("Glow around pressed keys.")
        .defaultValue(new SettingColor(255, 40, 40, 120))
        .build()
    );

    private final Setting<SettingColor> textColor = sgColors.add(new ColorSetting.Builder()
        .name("text")
        .description("Label color of a released key.")
        .defaultValue(new SettingColor(255, 170, 170))
        .build()
    );

    private final Setting<SettingColor> pressedTextColor = sgColors.add(new ColorSetting.Builder()
        .name("pressed-text")
        .description("Label color of a pressed key.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> shadowColor = sgColors.add(new ColorSetting.Builder()
        .name("shadow")
        .description("Soft drop shadow under each key.")
        .defaultValue(new SettingColor(0, 0, 0, 70))
        .build()
    );

    // State

    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();

    // forward, left, back, right, attack, use, jump, sneak
    private final double[] anim = new double[8];

    public Keystrokes() {
        super(Categories.Visuals, "keystrokes", "Shows your movement keys, mouse buttons and CPS.");
    }

    @Override
    public void onActivate() {
        leftClicks.clear();
        rightClicks.clear();
        java.util.Arrays.fill(anim, 0);
    }

    @EventHandler
    private void onMouseButton(MouseButtonEvent event) {
        if (event.action != KeyAction.Press || mc.currentScreen != null) return;
        long now = System.currentTimeMillis();
        if (event.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) leftClicks.addLast(now);
        else if (event.button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) rightClicks.addLast(now);
    }

    private static int cps(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.pollFirst();
        return clicks.size();
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null) return;
        double dt = VetmoRender.dt(event.frameTime);
        long now = System.currentTimeMillis();
        int lCps = cps(leftClicks, now);
        int rCps = cps(rightClicks, now);

        KeyBinding[] keys = {
            mc.options.forwardKey, mc.options.leftKey, mc.options.backKey, mc.options.rightKey,
            mc.options.attackKey, mc.options.useKey, mc.options.jumpKey, mc.options.sneakKey
        };
        for (int i = 0; i < keys.length; i++) {
            boolean pressed = keys[i].isPressed();
            // fast attack, slower configurable release fade
            anim[i] = VetmoRender.approach(anim[i], pressed ? 1 : 0, pressed ? 30 : fadeSpeed.get(), dt);
        }

        if (!VetmoRender.shouldRender(showInScreens.get())) return;

        DrawContext ctx = event.drawContext;
        int k = keySize.get();
        int g = gap.get();
        int totalW = k * 3 + g * 2;

        // compute total height
        int h = 0;
        if (showMovement.get()) h += (k + g) * 2;
        if (showMouse.get()) h += k + g;
        if (showSpace.get() || showSneak.get()) h += k / 2 + 2 + g;
        if (h == 0) return;
        h -= g;

        double s = scale.get();
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        float x = (float) (sw * posX.get() / 100.0);
        float y = (float) (sh * posY.get() / 100.0);
        x = (float) Math.max(2, Math.min(x, sw - totalW * s - 2));
        y = (float) Math.max(2, Math.min(y, sh - h * s - 2));

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale((float) s, (float) s);

        int cy = 0;
        if (showMovement.get()) {
            drawKey(ctx, k + g, cy, k, k, label(keys[0], "W"), null, anim[0]);
            cy += k + g;
            drawKey(ctx, 0, cy, k, k, label(keys[1], "A"), null, anim[1]);
            drawKey(ctx, k + g, cy, k, k, label(keys[2], "S"), null, anim[2]);
            drawKey(ctx, (k + g) * 2, cy, k, k, label(keys[3], "D"), null, anim[3]);
            cy += k + g;
        }

        if (showMouse.get()) {
            int half = (totalW - g) / 2;
            boolean cpsOn = showCps.get();
            drawKey(ctx, 0, cy, half, k, "LMB", cpsOn ? lCps + " CPS" : null, anim[4]);
            drawKey(ctx, half + g, cy, totalW - half - g, k, "RMB", cpsOn ? rCps + " CPS" : null, anim[5]);
            cy += k + g;
        }

        if (showSpace.get() || showSneak.get()) {
            int sh2 = k / 2 + 2;
            if (showSpace.get() && showSneak.get()) {
                int spaceW = totalW - k - g;
                drawSpace(ctx, 0, cy, spaceW, sh2, anim[6]);
                drawKey(ctx, spaceW + g, cy, k, sh2, label(keys[7], "SHF"), null, anim[7]);
            } else if (showSpace.get()) {
                drawSpace(ctx, 0, cy, totalW, sh2, anim[6]);
            } else {
                drawKey(ctx, 0, cy, totalW, sh2, label(keys[7], "SHIFT"), null, anim[7]);
            }
        }

        m.popMatrix();
    }

    private String label(KeyBinding key, String fallback) {
        if (!boundNames.get()) return fallback;
        String s = key.getBoundKeyLocalizedText().getString();
        if (s.isEmpty()) return fallback;
        if (s.length() > 3) {
            String[] words = s.trim().split("\\s+");
            if (words.length > 1) {
                StringBuilder sb = new StringBuilder();
                for (String word : words) if (!word.isEmpty() && sb.length() < 3) sb.append(word.charAt(0));
                s = sb.toString();
            } else s = s.substring(0, 3);
        }
        return s.toUpperCase(Locale.ROOT);
    }

    private void drawBox(DrawContext ctx, int x, int y, int w, int h, double p) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        // subtle "press in" scale
        float sc = (float) (1 - 0.06 * p);
        m.translate(x + w / 2f, y + h / 2f);
        m.scale(sc, sc);
        m.translate(-w / 2f, -h / 2f);

        VetmoRender.shadow(ctx, 0, 0, w, h, 3, 2, shadowColor.get().getPacked());
        if (p > 0.01) VetmoRender.shadow(ctx, -1, -1, w + 1, h + 1, 4, 3, VetmoRender.withAlpha(glowColor.get().getPacked(), p));

        int bg = VetmoRender.lerpColor(p, keyColor.get().getPacked(), pressedColor.get().getPacked());
        // same alpha, rgb darkened by 35% towards the bottom
        int bgBottom = (bg & 0xFF000000) | (VetmoRender.lerpColor(0.35, bg | 0xFF000000, 0xFF000000) & 0x00FFFFFF);
        VetmoRender.roundedGradient(ctx, 0, 0, w, h, 3, bg, bgBottom);
        VetmoRender.ring(ctx, 0, 0, w, h, 3, 1, VetmoRender.lerpColor(p, outlineColor.get().getPacked(), glowColor.get().getPacked() | 0xFF000000));
        m.popMatrix();
    }

    private void drawKey(DrawContext ctx, int x, int y, int w, int h, String text, String sub, double p) {
        drawBox(ctx, x, y, w, h, p);
        TextRenderer tr = mc.textRenderer;
        int color = VetmoRender.lerpColor(p, textColor.get().getPacked(), pressedTextColor.get().getPacked());

        if (sub == null) {
            VetmoRender.text(ctx, text, x + (w - tr.getWidth(text)) / 2, y + (h - 8) / 2, color, true);
        } else {
            int ty = y + h / 2 - 8;
            VetmoRender.text(ctx, text, x + (w - tr.getWidth(text)) / 2, ty, color, true);
            Matrix3x2fStack m = ctx.getMatrices();
            m.pushMatrix();
            float sx = x + w / 2f;
            m.translate(sx, ty + 10);
            m.scale(0.7f, 0.7f);
            VetmoRender.text(ctx, sub, -tr.getWidth(sub) / 2, 0, VetmoRender.withAlpha(color, 0.85), true);
            m.popMatrix();
        }
    }

    private void drawSpace(DrawContext ctx, int x, int y, int w, int h, double p) {
        drawBox(ctx, x, y, w, h, p);
        int color = VetmoRender.lerpColor(p, textColor.get().getPacked(), pressedTextColor.get().getPacked());
        int lw = Math.max(8, w / 3);
        int lx = x + (w - lw) / 2;
        int ly = y + h / 2;
        ctx.fill(lx, ly, lx + lw, ly + 1, color);
    }
}
