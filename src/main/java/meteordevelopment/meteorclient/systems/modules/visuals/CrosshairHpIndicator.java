/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.XAnchor;
import meteordevelopment.meteorclient.systems.hud.YAnchor;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.visuals.hud.VetmoHudModule;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.Locale;

/**
 * Shows your own health (and absorption) next to the crosshair, as a number with one decimal or as hearts.
 * Replacement for the "Crosshair Health Indicator" mod.
 *
 * <p>The HUD box has a fixed size (room for "88.8" and the absorption line) so the anchor does not jump when
 * absorption appears. Text is right-aligned so the top-right corner of the block sits next to the crosshair.</p>
 */
public class CrosshairHpIndicator extends Module implements VetmoHudModule {
    private static final Identifier HEART_CONTAINER = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.ofVanilla("hud/heart/half");
    private static final Identifier ABSORBING_FULL = Identifier.ofVanilla("hud/heart/absorbing_full");
    private static final Identifier ABSORBING_HALF = Identifier.ofVanilla("hud/heart/absorbing_half");

    /** Default top-right corner of the block relative to the screen center, in GUI pixels. */
    private static final int DEFAULT_RIGHT = -18, DEFAULT_TOP = -10;

    private static final int HEARTS_PER_ROW = 10;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<DisplayMode> mode = sgGeneral.add(new EnumSetting.Builder<DisplayMode>()
        .name("mode")
        .description("Show health as a number or as hearts.")
        .defaultValue(DisplayMode.Number)
        .build()
    );

    private final Setting<Boolean> absorption = sgGeneral.add(new BoolSetting.Builder()
        .name("absorption")
        .description("Shows absorption under the health (only when you have any).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> lowHealth = sgGeneral.add(new BoolSetting.Builder()
        .name("low-health-color")
        .description("Colors the health number when it is at or below the threshold.")
        .defaultValue(true)
        .visible(() -> mode.get() == DisplayMode.Number)
        .build()
    );

    private final Setting<Double> lowHealthThreshold = sgGeneral.add(new DoubleSetting.Builder()
        .name("low-health-threshold")
        .description("Health at or below which the low health color is used.")
        .defaultValue(6)
        .range(0.5, 40)
        .sliderRange(1, 20)
        .visible(() -> mode.get() == DisplayMode.Number && lowHealth.get())
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("text-shadow")
        .description("Draws the text with a shadow.")
        .defaultValue(true)
        .visible(() -> mode.get() == DisplayMode.Number)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Size of the indicator (mouse wheel over the element in the HUD editor).")
        .defaultValue(1)
        .min(0.3)
        .max(4)
        .sliderRange(0.5, 3)
        .build()
    );

    // Colors

    private final Setting<SettingColor> healthColor = sgColors.add(new ColorSetting.Builder()
        .name("health-color")
        .description("Color of the health number.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(() -> mode.get() == DisplayMode.Number)
        .build()
    );

    private final Setting<SettingColor> lowHealthColor = sgColors.add(new ColorSetting.Builder()
        .name("low-health-color")
        .description("Color of the health number at low health.")
        .defaultValue(new SettingColor(255, 40, 40))
        .visible(() -> mode.get() == DisplayMode.Number && lowHealth.get())
        .build()
    );

    private final Setting<SettingColor> absorptionColor = sgColors.add(new ColorSetting.Builder()
        .name("absorption-color")
        .description("Color of the absorption number.")
        .defaultValue(new SettingColor(212, 175, 55))
        .visible(() -> mode.get() == DisplayMode.Number && absorption.get())
        .build()
    );

    public CrosshairHpIndicator() {
        super(Categories.Visuals, "crosshair-hp-indicator", "Shows your health and absorption next to the crosshair.");
    }

    // HUD

    @Override
    public Setting<Double> hudScale() {
        return scale;
    }

    @Override
    public boolean shouldRenderHud() {
        return mc.player != null && mc.currentScreen == null && !mc.player.isSpectator();
    }

    private int lineHeight() {
        return mc.textRenderer.fontHeight + 1;
    }

    @Override
    public double getHudWidth() {
        if (mode.get() == DisplayMode.Hearts) return HEARTS_PER_ROW * 8 + 1;

        TextRenderer tr = mc.textRenderer;
        int w = tr.getWidth("88.8");
        if (mc.player != null) {
            w = Math.max(w, tr.getWidth(format(mc.player.getHealth())));
            w = Math.max(w, tr.getWidth(format(mc.player.getAbsorptionAmount())));
        }
        return w + 1; // + shadow
    }

    @Override
    public double getHudHeight() {
        if (mode.get() == DisplayMode.Hearts) return healthRows() * 10 + 10 - 1;
        return lineHeight() * 2;
    }

    private int healthRows() {
        float max = mc.player != null ? mc.player.getMaxHealth() : 20f;
        int containers = Math.max(1, (int) Math.ceil(max / 2f));
        return (containers + HEARTS_PER_ROW - 1) / HEARTS_PER_ROW;
    }

    @Override
    public XAnchor defaultHudXAnchor() { return XAnchor.Center; }

    @Override
    public YAnchor defaultHudYAnchor() { return YAnchor.Center; }

    /** Box center = top-right corner (-18, -10) shifted by (-w/2, +h/2), converted to real pixels. */
    @Override
    public int defaultHudX() {
        double sf = mc.getWindow().getScaleFactor();
        double w = getHudWidth() * scale.get();
        return (int) Math.round((DEFAULT_RIGHT - w / 2.0) * sf);
    }

    @Override
    public int defaultHudY() {
        double sf = mc.getWindow().getScaleFactor();
        double h = getHudHeight() * scale.get();
        return (int) Math.round((DEFAULT_TOP + h / 2.0) * sf);
    }

    @Override
    public void renderHud(DrawContext context, float tickDelta, boolean inEditor) {
        float health, abs, maxHealth;
        if (mc.player != null) {
            health = mc.player.getHealth();
            abs = mc.player.getAbsorptionAmount();
            maxHealth = mc.player.getMaxHealth();
        } else {
            // Preview in the main menu.
            health = 17.5f;
            abs = 4f;
            maxHealth = 20f;
        }

        if (inEditor && mc.player != null && abs <= 0) abs = 4f; // show where the absorption line goes

        if (mode.get() == DisplayMode.Hearts) renderHearts(context, health, abs, maxHealth);
        else renderNumber(context, health, abs);
    }

    private void renderNumber(DrawContext context, float health, float abs) {
        TextRenderer tr = mc.textRenderer;
        int boxW = (int) getHudWidth();

        String hpText = format(health);
        int hpColor = (lowHealth.get() && health <= lowHealthThreshold.get() ? lowHealthColor.get() : healthColor.get()).getPacked();
        context.drawText(tr, hpText, boxW - 1 - tr.getWidth(hpText), 0, hpColor, shadow.get());

        if (absorption.get() && abs > 0) {
            String absText = format(abs);
            context.drawText(tr, absText, boxW - 1 - tr.getWidth(absText), lineHeight(), absorptionColor.get().getPacked(), shadow.get());
        }
    }

    private void renderHearts(DrawContext context, float health, float abs, float maxHealth) {
        int boxW = (int) getHudWidth();

        int containers = Math.max(1, (int) Math.ceil(maxHealth / 2f));
        int halves = (int) Math.ceil(health); // vanilla rounds health up to half hearts
        int rows = (containers + HEARTS_PER_ROW - 1) / HEARTS_PER_ROW;

        for (int i = 0; i < containers; i++) {
            int row = i / HEARTS_PER_ROW;
            int col = i % HEARTS_PER_ROW;
            int inRow = Math.min(HEARTS_PER_ROW, containers - row * HEARTS_PER_ROW);
            int x = boxW - inRow * 8 - 1 + col * 8; // right-aligned row
            int y = row * 10;

            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, x, y, 9, 9);
            int value = halves - i * 2;
            if (value >= 2) context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HEART_FULL, x, y, 9, 9);
            else if (value == 1) context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HEART_HALF, x, y, 9, 9);
        }

        if (absorption.get() && abs > 0) {
            int absHalves = (int) Math.ceil(abs);
            int absHearts = Math.min(HEARTS_PER_ROW, (absHalves + 1) / 2);
            int y = rows * 10;

            for (int i = 0; i < absHearts; i++) {
                int x = boxW - absHearts * 8 - 1 + i * 8;
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, HEART_CONTAINER, x, y, 9, 9);
                int value = absHalves - i * 2;
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, value >= 2 ? ABSORBING_FULL : ABSORBING_HALF, x, y, 9, 9);
            }
        }
    }

    private static String format(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    public enum DisplayMode {
        Number,
        Hearts
    }
}
