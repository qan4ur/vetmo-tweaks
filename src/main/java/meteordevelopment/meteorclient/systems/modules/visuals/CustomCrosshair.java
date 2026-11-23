/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces the vanilla crosshair. Drawn from {@code VetmoInGameHudMixin} (InGameHud#renderCrosshair HEAD, cancelled).
 * Everything is drawn in real framebuffer pixels so the shapes stay crisp at any GUI scale.
 */
public class CustomCrosshair extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgDynamic = settings.createGroup("Dynamic");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Style> style = sgGeneral.add(new EnumSetting.Builder<Style>()
        .name("style")
        .description("The shape of the crosshair.")
        .defaultValue(Style.Cross)
        .build()
    );

    private final Setting<Boolean> scaleWithGui = sgGeneral.add(new BoolSetting.Builder()
        .name("scale-with-gui")
        .description("Measure sizes in GUI pixels (scales with GUI scale) instead of real screen pixels.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> size = sgGeneral.add(new IntSetting.Builder()
        .name("size")
        .description("Length of the cross arms / radius of the circle.")
        .defaultValue(4)
        .range(1, 64)
        .sliderRange(1, 20)
        .build()
    );

    private final Setting<Integer> gap = sgGeneral.add(new IntSetting.Builder()
        .name("gap")
        .description("Empty space between the center and the arms.")
        .defaultValue(2)
        .range(0, 64)
        .sliderRange(0, 15)
        .build()
    );

    private final Setting<Integer> thickness = sgGeneral.add(new IntSetting.Builder()
        .name("thickness")
        .description("Thickness of the lines.")
        .defaultValue(1)
        .range(1, 16)
        .sliderRange(1, 6)
        .build()
    );

    private final Setting<Integer> dotSize = sgGeneral.add(new IntSetting.Builder()
        .name("dot-size")
        .description("Size of the center dot.")
        .defaultValue(1)
        .range(1, 16)
        .sliderRange(1, 6)
        .visible(() -> style.get() == Style.Dot || style.get() == Style.CrossDot)
        .build()
    );

    private final Setting<Boolean> outline = sgGeneral.add(new BoolSetting.Builder()
        .name("outline")
        .description("Draws an outline around the crosshair.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> outlineWidth = sgGeneral.add(new IntSetting.Builder()
        .name("outline-width")
        .description("Width of the outline.")
        .defaultValue(1)
        .range(1, 8)
        .sliderRange(1, 4)
        .visible(outline::get)
        .build()
    );

    // Dynamic

    private final Setting<Boolean> dynamicSpread = sgDynamic.add(new BoolSetting.Builder()
        .name("attack-cooldown-spread")
        .description("Spreads the crosshair out while your attack is on cooldown.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> spreadAmount = sgDynamic.add(new IntSetting.Builder()
        .name("spread-amount")
        .description("How far the crosshair spreads at zero attack charge.")
        .defaultValue(4)
        .range(1, 32)
        .sliderRange(1, 12)
        .visible(dynamicSpread::get)
        .build()
    );

    private final Setting<Boolean> targetColorEnabled = sgDynamic.add(new BoolSetting.Builder()
        .name("target-color")
        .description("Changes color when the crosshair is on an entity.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> playersOnly = sgDynamic.add(new BoolSetting.Builder()
        .name("players-only")
        .description("Only use the target color on players (otherwise any living entity).")
        .defaultValue(true)
        .visible(targetColorEnabled::get)
        .build()
    );

    // Colors

    private final Setting<SettingColor> color = sgColors.add(new ColorSetting.Builder()
        .name("color")
        .description("Crosshair color.")
        .defaultValue(new SettingColor(220, 20, 40))
        .build()
    );

    private final Setting<SettingColor> targetColor = sgColors.add(new ColorSetting.Builder()
        .name("target-color")
        .description("Crosshair color while aiming at a target.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(targetColorEnabled::get)
        .build()
    );

    private final Setting<SettingColor> outlineColor = sgColors.add(new ColorSetting.Builder()
        .name("outline-color")
        .description("Outline color.")
        .defaultValue(new SettingColor(130, 0, 15))
        .visible(outline::get)
        .build()
    );

    private final List<int[]> rects = new ArrayList<>();

    public CustomCrosshair() {
        super(Categories.Visuals, "custom-crosshair", "Replaces the vanilla crosshair with a customizable one.");
    }

    /** Whether the vanilla crosshair should be replaced. Debug-screen gizmo and spectator mode stay vanilla. */
    public boolean shouldReplace() {
        if (mc.player == null) return false;
        if (mc.player.isSpectator()) return false;
        return !mc.inGameHud.getDebugHud().shouldShowDebugHud();
    }

    public void render(DrawContext context, float tickDelta) {
        if (mc.player == null || !mc.options.getPerspective().isFirstPerson()) return;

        int sf = Math.max(1, mc.getWindow().getScaleFactor());
        int unit = scaleWithGui.get() ? sf : 1;

        int cx = mc.getWindow().getFramebufferWidth() / 2;
        int cy = mc.getWindow().getFramebufferHeight() / 2;

        int t = thickness.get() * unit;
        int s = size.get() * unit;
        int g = gap.get() * unit;

        if (dynamicSpread.get()) {
            float progress = mc.player.getAttackCooldownProgress(tickDelta);
            g += Math.round((1f - progress) * spreadAmount.get() * unit);
        }

        rects.clear();
        switch (style.get()) {
            case Cross -> addCross(cx, cy, s, g, t, true);
            case Dot -> addBlock(cx, cy, dotSize.get() * unit);
            case Circle -> addRing(cx, cy, s + g, t);
            case CrossDot -> {
                addCross(cx, cy, s, g, t, false);
                addBlock(cx, cy, dotSize.get() * unit);
            }
        }

        int mainColor = (isOnTarget() ? targetColor.get() : color.get()).getPacked();

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.scale(1f / sf, 1f / sf);

        if (outline.get()) {
            int ow = outlineWidth.get() * unit;
            int oc = outlineColor.get().getPacked();
            for (int[] r : rects) context.fill(r[0] - ow, r[1] - ow, r[2] + ow, r[3] + ow, oc);
        }
        for (int[] r : rects) context.fill(r[0], r[1], r[2], r[3], mainColor);

        matrices.popMatrix();
    }

    private boolean isOnTarget() {
        if (!targetColorEnabled.get()) return false;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return false;
        if (playersOnly.get()) return hit.getEntity() instanceof PlayerEntity;
        return hit.getEntity() instanceof LivingEntity;
    }

    /** Center block covers [c - t/2, c - t/2 + t) on both axes; arms are placed symmetrically around it. */
    private void addCross(int cx, int cy, int s, int g, int t, boolean fillCenterOnZeroGap) {
        int lo = -(t / 2);
        int hi = lo + t;

        rects.add(new int[]{cx + lo - g - s, cy + lo, cx + lo - g, cy + hi}); // left
        rects.add(new int[]{cx + hi + g, cy + lo, cx + hi + g + s, cy + hi}); // right
        rects.add(new int[]{cx + lo, cy + lo - g - s, cx + hi, cy + lo - g}); // top
        rects.add(new int[]{cx + lo, cy + hi + g, cx + hi, cy + hi + g + s}); // bottom

        if (fillCenterOnZeroGap && g == 0) rects.add(new int[]{cx + lo, cy + lo, cx + hi, cy + hi});
    }

    private void addBlock(int cx, int cy, int d) {
        int lo = -(d / 2);
        rects.add(new int[]{cx + lo, cy + lo, cx + lo + d, cy + lo + d});
    }

    /** Pixel-exact ring built from horizontal runs. Pixel (x, y) is inside if innerR <= dist(center pixel) < outerR. */
    private void addRing(int cx, int cy, int radius, int t) {
        double outer = radius + t / 2.0;
        double inner = Math.max(0, radius - t / 2.0);
        double outer2 = outer * outer, inner2 = inner * inner;
        int r = (int) Math.ceil(outer) + 1;

        for (int dy = -r; dy <= r; dy++) {
            int runStart = Integer.MIN_VALUE;
            for (int dx = -r; dx <= r + 1; dx++) {
                double d2 = dx * dx + dy * dy;
                boolean in = dx <= r && d2 < outer2 && d2 >= inner2;
                if (in && runStart == Integer.MIN_VALUE) runStart = dx;
                else if (!in && runStart != Integer.MIN_VALUE) {
                    rects.add(new int[]{cx + runStart, cy + dy, cx + dx, cy + dy + 1});
                    runStart = Integer.MIN_VALUE;
                }
            }
        }
    }

    public enum Style {
        Cross("Cross"),
        Dot("Dot"),
        Circle("Circle"),
        CrossDot("Cross + Dot");

        private final String title;

        Style(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
