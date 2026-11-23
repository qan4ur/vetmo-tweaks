/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;

/**
 * Small drawing/animation helpers shared by the Vetmo HUD modules (ArmorHud, PotionHud, CooldownsHud, TotemPopCounter).
 */
public final class VetmoHudRender {
    public enum Anchor { TopLeft, TopCenter, TopRight, MiddleLeft, MiddleRight, BottomLeft, BottomCenter, BottomRight }

    private VetmoHudRender() {
    }

    /** Dark panel with 1px rounded corners, a faint accent glow and an accent top line. */
    public static void drawPanel(DrawContext ctx, int x, int y, int w, int h, SettingColor bg, SettingColor accent, float ease) {
        int bgc = withAlpha(bg.getPacked(), (int) (bg.a * ease));
        int glow = withAlpha(accent.getPacked(), (int) (40 * ease));
        ctx.fill(x - 1, y + 1, x, y + h - 1, glow);
        ctx.fill(x + w, y + 1, x + w + 1, y + h - 1, glow);
        ctx.fill(x + 1, y + h, x + w - 1, y + h + 1, glow);

        ctx.fill(x + 1, y, x + w - 1, y + h, bgc);
        ctx.fill(x, y + 1, x + 1, y + h - 1, bgc);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, bgc);

        int ac = withAlpha(accent.getPacked(), (int) (accent.a * ease));
        int half = w / 2;
        ctx.fillGradient(x + 1, y, x + half, y + 1, withAlpha(ac, (int) (60 * ease)), ac);
        ctx.fill(x + half, y, x + w - 1, y + 1, ac);
    }

    /** Top-left corner for a box of size w*h attached to an anchor with an offset. */
    public static float[] anchored(DrawContext ctx, Anchor anchor, int ox, int oy, float w, float h) {
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        float x, y;
        switch (anchor) {
            case TopLeft -> { x = ox; y = oy; }
            case TopCenter -> { x = (sw - w) / 2 + ox; y = oy; }
            case TopRight -> { x = sw - w - ox; y = oy; }
            case MiddleLeft -> { x = ox; y = (sh - h) / 2 + oy; }
            case MiddleRight -> { x = sw - w - ox; y = (sh - h) / 2 + oy; }
            case BottomLeft -> { x = ox; y = sh - h - oy; }
            case BottomRight -> { x = sw - w - ox; y = sh - h - oy; }
            default -> { x = (sw - w) / 2 + ox; y = sh - h - oy; }
        }
        return new float[] { x, y };
    }

    public static boolean isRightSide(Anchor anchor) {
        return anchor == Anchor.TopRight || anchor == Anchor.MiddleRight || anchor == Anchor.BottomRight;
    }

    public static boolean isBottom(Anchor anchor) {
        return anchor == Anchor.BottomLeft || anchor == Anchor.BottomCenter || anchor == Anchor.BottomRight;
    }

    /** Frame-rate independent exponential approach. */
    public static float approach(float current, float target, double frameTime, float speed) {
        float k = 1 - (float) Math.exp(-speed * Math.max(frameTime, 0));
        return current + (target - current) * k;
    }

    public static float easeOutCubic(float t) {
        t = MathHelper.clamp(t, 0, 1);
        float f = 1 - t;
        return 1 - f * f * f;
    }

    public static float easeOutBack(float t) {
        t = MathHelper.clamp(t, 0, 1);
        float c1 = 1.70158f, c3 = c1 + 1;
        float f = t - 1;
        return 1 + c3 * f * f * f + c1 * f * f;
    }

    /** 0..1 sine pulse, `speed` pulses per second. */
    public static float pulse(double speed) {
        double time = System.nanoTime() / 1e9;
        return (float) (0.5 + 0.5 * Math.sin(time * Math.PI * 2 * speed));
    }

    public static int withAlpha(int argb, int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }

    /** Multiplies the color's own alpha by f. */
    public static int fade(int argb, float f) {
        return withAlpha(argb, (int) ((argb >>> 24) * MathHelper.clamp(f, 0, 1)));
    }

    public static int lerpColor(int a, int b, float t) {
        t = MathHelper.clamp(t, 0, 1);
        int aa = a >>> 24, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int ba = b >>> 24, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
