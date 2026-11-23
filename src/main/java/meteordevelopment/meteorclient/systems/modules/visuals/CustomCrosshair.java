/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.XAnchor;
import meteordevelopment.meteorclient.systems.hud.YAnchor;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.visuals.hud.VetmoHudElement;
import meteordevelopment.meteorclient.systems.modules.visuals.hud.VetmoHudModule;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces the vanilla crosshair and the attack indicator under it with versions drawn in their real colors
 * (vanilla uses the inverting CROSSHAIR render pipeline, which is invisible on grey backgrounds).
 *
 * <p>Vanilla {@code InGameHud#renderCrosshair} is cancelled by {@code VetmoInGameHudMixin} while this module is
 * active and its HUD element will actually be drawn; the drawing itself happens in {@link #renderHud} as a HUD
 * element centered on the screen. With the debug screen (F3) open the vanilla crosshair / axes stay.</p>
 *
 * <p>Everything is drawn in GUI units around a single "center pixel" in the middle of an odd-sized box, so at
 * scale 1 the shapes are pixel-exact and line up with the vanilla crosshair position.</p>
 */
public class CustomCrosshair extends Module implements VetmoHudModule {
    private static final Identifier CROSSHAIR_TEXTURE = Identifier.ofVanilla("hud/crosshair");
    private static final Identifier INDICATOR_FULL_TEXTURE = Identifier.ofVanilla("hud/crosshair_attack_indicator_full");
    private static final Identifier INDICATOR_BACKGROUND_TEXTURE = Identifier.ofVanilla("hud/crosshair_attack_indicator_background");
    private static final Identifier INDICATOR_PROGRESS_TEXTURE = Identifier.ofVanilla("hud/crosshair_attack_indicator_progress");

    private static final long FLASH_MS = 250;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCustom = settings.createGroup("Custom Shape");
    private final SettingGroup sgDynamic = settings.createGroup("Dynamic");
    private final SettingGroup sgIndicator = settings.createGroup("Attack Indicator");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Boolean> textures = sgGeneral.add(new BoolSetting.Builder()
        .name("resource-pack-textures")
        .description("Draws the crosshair and attack indicator sprites of the active resource pack in their own colors (without color inversion) instead of the shapes below.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Style> style = sgGeneral.add(new EnumSetting.Builder<Style>()
        .name("style")
        .description("The shape of the crosshair.")
        .defaultValue(Style.Cross)
        .visible(() -> !textures.get())
        .build()
    );

    private final Setting<Integer> size = sgGeneral.add(new IntSetting.Builder()
        .name("size")
        .description("Length of the cross arms / radius of the circle (GUI pixels).")
        .defaultValue(4)
        .range(1, 32)
        .sliderRange(1, 16)
        .visible(() -> !textures.get() && style.get() != Style.Dot && style.get() != Style.Custom)
        .build()
    );

    private final Setting<Integer> gap = sgGeneral.add(new IntSetting.Builder()
        .name("gap")
        .description("Empty space between the center and the arms (GUI pixels).")
        .defaultValue(2)
        .range(0, 32)
        .sliderRange(0, 12)
        .visible(() -> !textures.get() && style.get() != Style.Dot)
        .build()
    );

    private final Setting<Integer> thickness = sgGeneral.add(new IntSetting.Builder()
        .name("thickness")
        .description("Thickness of the lines (GUI pixels).")
        .defaultValue(1)
        .range(1, 8)
        .sliderRange(1, 5)
        .visible(() -> !textures.get() && style.get() != Style.Dot)
        .build()
    );

    private final Setting<Integer> dotSize = sgGeneral.add(new IntSetting.Builder()
        .name("dot-size")
        .description("Size of the center dot (GUI pixels).")
        .defaultValue(1)
        .range(1, 8)
        .sliderRange(1, 5)
        .visible(() -> !textures.get() && hasDot())
        .build()
    );

    private final Setting<Boolean> outline = sgGeneral.add(new BoolSetting.Builder()
        .name("outline")
        .description("Draws an outline around the crosshair.")
        .defaultValue(true)
        .visible(() -> !textures.get())
        .build()
    );

    private final Setting<Integer> outlineWidth = sgGeneral.add(new IntSetting.Builder()
        .name("outline-width")
        .description("Width of the outline (GUI pixels).")
        .defaultValue(1)
        .range(1, 4)
        .sliderRange(1, 3)
        .visible(() -> !textures.get() && outline.get())
        .build()
    );

    private final Setting<Double> opacity = sgGeneral.add(new DoubleSetting.Builder()
        .name("opacity")
        .description("Opacity of the crosshair and the attack indicator.")
        .defaultValue(1)
        .range(0.05, 1)
        .sliderRange(0.05, 1)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("HUD scale (mouse wheel over the element in the HUD editor). 1 = vanilla pixel size.")
        .defaultValue(1)
        .min(0.3)
        .max(4)
        .sliderRange(0.5, 3)
        .build()
    );

    // Custom shape

    private final Setting<Integer> topLength = sgCustom.add(new IntSetting.Builder()
        .name("top-length")
        .description("Length of the top arm (0 = no arm, gives a T shape).")
        .defaultValue(4)
        .range(0, 32)
        .sliderRange(0, 16)
        .visible(this::isCustom)
        .build()
    );

    private final Setting<Integer> bottomLength = sgCustom.add(new IntSetting.Builder()
        .name("bottom-length")
        .description("Length of the bottom arm (0 = no arm).")
        .defaultValue(4)
        .range(0, 32)
        .sliderRange(0, 16)
        .visible(this::isCustom)
        .build()
    );

    private final Setting<Integer> leftLength = sgCustom.add(new IntSetting.Builder()
        .name("left-length")
        .description("Length of the left arm (0 = no arm).")
        .defaultValue(4)
        .range(0, 32)
        .sliderRange(0, 16)
        .visible(this::isCustom)
        .build()
    );

    private final Setting<Integer> rightLength = sgCustom.add(new IntSetting.Builder()
        .name("right-length")
        .description("Length of the right arm (0 = no arm).")
        .defaultValue(4)
        .range(0, 32)
        .sliderRange(0, 16)
        .visible(this::isCustom)
        .build()
    );

    private final Setting<Boolean> customDot = sgCustom.add(new BoolSetting.Builder()
        .name("center-dot")
        .description("Draws a dot in the center.")
        .defaultValue(false)
        .visible(this::isCustom)
        .build()
    );

    // Dynamic

    private final Setting<Boolean> dynamicSpread = sgDynamic.add(new BoolSetting.Builder()
        .name("attack-cooldown-spread")
        .description("Spreads the crosshair out while your attack is on cooldown.")
        .defaultValue(true)
        .visible(() -> !textures.get())
        .build()
    );

    private final Setting<Integer> spreadAmount = sgDynamic.add(new IntSetting.Builder()
        .name("spread-amount")
        .description("How far the crosshair spreads at zero attack charge (GUI pixels).")
        .defaultValue(4)
        .range(1, 16)
        .sliderRange(1, 10)
        .visible(() -> !textures.get() && dynamicSpread.get())
        .build()
    );

    private final Setting<Boolean> targetColorEnabled = sgDynamic.add(new BoolSetting.Builder()
        .name("target-color")
        .description("Changes the crosshair color while aiming at a target.")
        .defaultValue(true)
        .visible(() -> !textures.get())
        .build()
    );

    private final Setting<Boolean> playersOnly = sgDynamic.add(new BoolSetting.Builder()
        .name("players-only")
        .description("Only use the target color on players (otherwise any living entity).")
        .defaultValue(true)
        .visible(() -> !textures.get() && targetColorEnabled.get())
        .build()
    );

    // Attack indicator

    private final Setting<IndicatorMode> indicatorMode = sgIndicator.add(new EnumSetting.Builder<IndicatorMode>()
        .name("mode")
        .description("When to draw the attack cooldown bar under the crosshair.")
        .defaultValue(IndicatorMode.FollowVanilla)
        .build()
    );

    private final Setting<Integer> indicatorOffset = sgIndicator.add(new IntSetting.Builder()
        .name("offset")
        .description("Distance from the crosshair center to the top of the bar (GUI pixels). 10 = vanilla.")
        .defaultValue(10)
        .range(2, 40)
        .sliderRange(4, 24)
        .visible(() -> indicatorMode.get() != IndicatorMode.Off)
        .build()
    );

    private final Setting<Integer> indicatorWidth = sgIndicator.add(new IntSetting.Builder()
        .name("width")
        .description("Width of the bar without outline (GUI pixels).")
        .defaultValue(15)
        .range(3, 61)
        .sliderRange(5, 31)
        .visible(() -> !textures.get() && indicatorMode.get() != IndicatorMode.Off)
        .build()
    );

    private final Setting<Integer> indicatorHeight = sgIndicator.add(new IntSetting.Builder()
        .name("height")
        .description("Height of the bar without outline (GUI pixels).")
        .defaultValue(2)
        .range(1, 8)
        .sliderRange(1, 5)
        .visible(() -> !textures.get() && indicatorMode.get() != IndicatorMode.Off)
        .build()
    );

    private final Setting<Boolean> indicatorOutline = sgIndicator.add(new BoolSetting.Builder()
        .name("outline")
        .description("Draws a 1 pixel outline around the bar.")
        .defaultValue(true)
        .visible(() -> !textures.get() && indicatorMode.get() != IndicatorMode.Off)
        .build()
    );

    private final Setting<Boolean> indicatorFlash = sgIndicator.add(new BoolSetting.Builder()
        .name("flash-when-full")
        .description("Flashes the bar briefly when the attack becomes fully charged.")
        .defaultValue(true)
        .visible(() -> !textures.get() && indicatorMode.get() != IndicatorMode.Off)
        .build()
    );

    // Colors

    private final Setting<SettingColor> color = sgColors.add(new ColorSetting.Builder()
        .name("color")
        .description("Crosshair color.")
        .defaultValue(new SettingColor(220, 20, 40))
        .visible(() -> !textures.get())
        .build()
    );

    private final Setting<SettingColor> targetColor = sgColors.add(new ColorSetting.Builder()
        .name("target-color")
        .description("Crosshair color while aiming at a target.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(() -> !textures.get() && targetColorEnabled.get())
        .build()
    );

    private final Setting<SettingColor> outlineColor = sgColors.add(new ColorSetting.Builder()
        .name("outline-color")
        .description("Crosshair outline color.")
        .defaultValue(new SettingColor(0, 0, 0))
        .visible(() -> !textures.get() && outline.get())
        .build()
    );

    private final Setting<SettingColor> barColor = sgColors.add(new ColorSetting.Builder()
        .name("bar-color")
        .description("Attack indicator fill color while charging.")
        .defaultValue(new SettingColor(220, 20, 40))
        .visible(() -> !textures.get() && indicatorMode.get() != IndicatorMode.Off)
        .build()
    );

    private final Setting<SettingColor> barFullColor = sgColors.add(new ColorSetting.Builder()
        .name("bar-full-color")
        .description("Attack indicator color when fully charged.")
        .defaultValue(new SettingColor(255, 90, 90))
        .visible(() -> !textures.get() && indicatorMode.get() != IndicatorMode.Off)
        .build()
    );

    private final Setting<SettingColor> barFlashColor = sgColors.add(new ColorSetting.Builder()
        .name("bar-flash-color")
        .description("Color the bar flashes with when it becomes fully charged.")
        .defaultValue(new SettingColor(255, 255, 255))
        .visible(() -> !textures.get() && indicatorMode.get() != IndicatorMode.Off && indicatorFlash.get())
        .build()
    );

    private final Setting<SettingColor> barBackground = sgColors.add(new ColorSetting.Builder()
        .name("bar-background")
        .description("Color of the empty part of the attack indicator.")
        .defaultValue(new SettingColor(60, 0, 8, 200))
        .visible(() -> !textures.get() && indicatorMode.get() != IndicatorMode.Off)
        .build()
    );

    private final Setting<SettingColor> barOutlineColor = sgColors.add(new ColorSetting.Builder()
        .name("bar-outline-color")
        .description("Color of the attack indicator outline.")
        .defaultValue(new SettingColor(0, 0, 0))
        .visible(() -> !textures.get() && indicatorMode.get() != IndicatorMode.Off && indicatorOutline.get())
        .build()
    );

    private final List<int[]> rects = new ArrayList<>();
    private boolean wasFull = true;
    private long flashStart;

    public CustomCrosshair() {
        super(Categories.Visuals, "custom-crosshair", "Replaces the vanilla crosshair and attack indicator with non-inverted, customizable ones.");
    }

    private boolean isCustom() {
        return !textures.get() && style.get() == Style.Custom;
    }

    private boolean hasDot() {
        return style.get() == Style.Dot || style.get() == Style.CrossDot || (style.get() == Style.Custom && customDot.get());
    }

    // Vanilla replacement

    /**
     * Whether vanilla {@code renderCrosshair} should be cancelled. Not with the debug screen open (the HUD is hidden
     * then and vanilla draws its axes / crosshair), and not when our HUD element would not be drawn at all.
     */
    public boolean shouldCancelVanilla() {
        if (mc.player == null || mc.inGameHud == null) return false;
        if (mc.inGameHud.getDebugHud().shouldShowDebugHud()) return false;
        return hudWillRender();
    }

    private boolean hudWillRender() {
        Hud hud = Hud.get();
        if (hud == null) return false;

        if (mc.currentScreen != null && !(mc.currentScreen instanceof WidgetScreen)) {
            Setting<?> hideInMenus = hud.settings.get("hide-in-menus");
            if (hideInMenus != null && Boolean.TRUE.equals(hideInMenus.get())) return false;
        }

        for (HudElement element : hud) {
            if (element instanceof VetmoHudElement e && e.getModule() == this) return element.isActive();
        }
        return false;
    }

    /** Mirrors the conditions of vanilla InGameHud#renderCrosshair (perspective, debug screen, spectator). */
    private boolean vanillaWouldDraw() {
        if (mc.player == null || mc.interactionManager == null) return false;
        if (!mc.options.getPerspective().isFirstPerson()) return false;
        if (mc.inGameHud.getDebugHud().shouldShowDebugHud()) return false;
        return mc.interactionManager.getCurrentGameMode() != GameMode.SPECTATOR || shouldRenderSpectatorCrosshair(mc.crosshairTarget);
    }

    /** Copy of the private vanilla InGameHud#shouldRenderSpectatorCrosshair. */
    private boolean shouldRenderSpectatorCrosshair(HitResult hit) {
        if (hit == null) return false;
        if (hit.getType() == HitResult.Type.ENTITY) return ((EntityHitResult) hit).getEntity() instanceof NamedScreenHandlerFactory;
        if (hit.getType() == HitResult.Type.BLOCK && mc.world != null) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            return mc.world.getBlockState(pos).createScreenHandlerFactory(mc.world, pos) != null;
        }
        return false;
    }

    // HUD

    @Override
    public Setting<Double> hudScale() {
        return scale;
    }

    @Override
    public boolean shouldRenderHud() {
        return vanillaWouldDraw();
    }

    @Override
    public int defaultHudX() { return 0; }

    @Override
    public int defaultHudY() { return 0; }

    @Override
    public XAnchor defaultHudXAnchor() { return XAnchor.Center; }

    @Override
    public YAnchor defaultHudYAnchor() { return YAnchor.Center; }

    /** Half extent (without the center pixel) of the crosshair shape, including max spread and outline. */
    private int crosshairExtent() {
        if (textures.get()) return 8;

        int o = outline.get() ? outlineWidth.get() : 0;
        int t = thickness.get();
        int g = gap.get() + (dynamicSpread.get() ? spreadAmount.get() : 0);
        int ext = switch (style.get()) {
            case Dot -> dotSize.get() / 2 + 1;
            case Circle -> size.get() + g + t / 2 + 2;
            case Custom -> g + t + Math.max(Math.max(topLength.get(), bottomLength.get()), Math.max(leftLength.get(), rightLength.get()));
            default -> g + t + size.get();
        };
        if (hasDot()) ext = Math.max(ext, dotSize.get() / 2 + 1);
        return ext + o;
    }

    private int indicatorHalfWidth() {
        if (indicatorMode.get() == IndicatorMode.Off) return 0;
        if (textures.get()) return 9;
        return indicatorWidth.get() / 2 + 2;
    }

    private int indicatorBottom() {
        if (indicatorMode.get() == IndicatorMode.Off) return 0;
        if (textures.get()) return indicatorOffset.get() + 16;
        return indicatorOffset.get() + indicatorHeight.get() + 2;
    }

    private int halfWidth() {
        return Math.max(crosshairExtent(), indicatorHalfWidth());
    }

    private int halfHeight() {
        return Math.max(crosshairExtent(), indicatorBottom());
    }

    @Override
    public double getHudWidth() {
        return halfWidth() * 2 + 1;
    }

    @Override
    public double getHudHeight() {
        return halfHeight() * 2 + 1;
    }

    @Override
    public void renderHud(DrawContext context, float tickDelta, boolean inEditor) {
        // Center pixel of the odd-sized box.
        int cx = halfWidth();
        int cy = halfHeight();

        float progress;
        if (mc.player != null) progress = mc.player.getAttackCooldownProgress(tickDelta);
        else progress = (System.currentTimeMillis() % 1500) / 1200f; // preview in the main menu
        progress = Math.min(1f, Math.max(0f, progress));

        if (textures.get()) renderTextures(context, cx, cy, progress, inEditor);
        else {
            renderShape(context, cx, cy, progress);
            renderIndicator(context, cx, cy, progress, inEditor);
        }
    }

    // Shapes

    private void renderShape(DrawContext context, int cx, int cy, float progress) {
        int t = thickness.get();
        int s = size.get();
        int g = gap.get();
        if (dynamicSpread.get()) g += Math.round((1f - progress) * spreadAmount.get());

        rects.clear();
        switch (style.get()) {
            case Cross -> addCross(cx, cy, s, s, s, s, g, t, true);
            case Dot -> addBlock(cx, cy, dotSize.get());
            case Circle -> addRing(cx, cy, s + g, t);
            case CrossDot -> {
                addCross(cx, cy, s, s, s, s, g, t, false);
                addBlock(cx, cy, dotSize.get());
            }
            case Custom -> {
                addCross(cx, cy, topLength.get(), bottomLength.get(), leftLength.get(), rightLength.get(), g, t, !customDot.get());
                if (customDot.get()) addBlock(cx, cy, dotSize.get());
            }
        }

        int mainColor = withOpacity((isOnTarget() ? targetColor.get() : color.get()).getPacked());

        if (outline.get()) {
            int ow = outlineWidth.get();
            int oc = withOpacity(outlineColor.get().getPacked());
            for (int[] r : rects) context.fill(r[0] - ow, r[1] - ow, r[2] + ow, r[3] + ow, oc);
        }
        for (int[] r : rects) context.fill(r[0], r[1], r[2], r[3], mainColor);
    }

    private boolean isOnTarget() {
        if (!targetColorEnabled.get()) return false;
        if (!(mc.crosshairTarget instanceof EntityHitResult hit)) return false;
        if (playersOnly.get()) return hit.getEntity() instanceof PlayerEntity;
        return hit.getEntity() instanceof LivingEntity;
    }

    /** Center block covers [c - t/2, c - t/2 + t) on both axes; arms are placed symmetrically around it. */
    private void addCross(int cx, int cy, int top, int bottom, int left, int right, int g, int t, boolean fillCenterOnZeroGap) {
        int lo = -(t / 2);
        int hi = lo + t;

        if (left > 0) rects.add(new int[]{cx + lo - g - left, cy + lo, cx + lo - g, cy + hi});
        if (right > 0) rects.add(new int[]{cx + hi + g, cy + lo, cx + hi + g + right, cy + hi});
        if (top > 0) rects.add(new int[]{cx + lo, cy + lo - g - top, cx + hi, cy + lo - g});
        if (bottom > 0) rects.add(new int[]{cx + lo, cy + hi + g, cx + hi, cy + hi + g + bottom});

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

    // Attack indicator

    private enum IndicatorState { Hidden, Charging, Full }

    /**
     * Vanilla: only with the CROSSHAIR attack indicator option; a bar while charging, the "full" sprite when looking
     * at a living, alive entity with full charge and an attack speed faster than 5 ticks, otherwise nothing.
     */
    private IndicatorState indicatorState(float progress, boolean inEditor) {
        IndicatorMode mode = indicatorMode.get();
        if (mode == IndicatorMode.Off) return IndicatorState.Hidden;

        IndicatorState preview = progress >= 1f ? IndicatorState.Full : IndicatorState.Charging;
        if (mc.player == null) return inEditor ? preview : IndicatorState.Hidden;
        if (mode == IndicatorMode.Always) return preview;

        if (mc.options.getAttackIndicator().getValue() != AttackIndicator.CROSSHAIR) {
            // Still preview it in the HUD editor so the element can be positioned.
            return inEditor ? preview : IndicatorState.Hidden;
        }

        float vanillaProgress = mc.player.getAttackCooldownProgress(0f);
        Entity target = mc.targetedEntity;
        boolean full = target instanceof LivingEntity && vanillaProgress >= 1f
            && mc.player.getAttackCooldownProgressPerTick() > 5f && target.isAlive();

        if (full) return IndicatorState.Full;
        if (vanillaProgress < 1f) return IndicatorState.Charging;
        return inEditor ? IndicatorState.Full : IndicatorState.Hidden;
    }

    private void renderIndicator(DrawContext context, int cx, int cy, float progress, boolean inEditor) {
        IndicatorState state = indicatorState(progress, inEditor);

        // Tracked even while hidden so the flash starts exactly when the charge completes.
        boolean isFull = progress >= 1f;
        if (isFull && !wasFull) flashStart = System.currentTimeMillis();
        wasFull = isFull;

        if (state == IndicatorState.Hidden) return;

        int w = indicatorWidth.get();
        int h = indicatorHeight.get();
        int x = cx - w / 2;
        int y = cy + indicatorOffset.get() + 1;

        if (indicatorOutline.get()) {
            context.fill(x - 1, y - 1, x + w + 1, y + h + 1, withOpacity(barOutlineColor.get().getPacked()));
        }

        if (state == IndicatorState.Full) {
            int c = barFullColor.get().getPacked();
            if (indicatorFlash.get()) {
                long since = System.currentTimeMillis() - flashStart;
                if (since >= 0 && since < FLASH_MS) c = lerpColor(barFlashColor.get().getPacked(), c, since / (float) FLASH_MS);
            }
            context.fill(x, y, x + w, y + h, withOpacity(c));
        } else {
            int filled = Math.min(w, (int) (progress * (w + 1)));
            context.fill(x, y, x + w, y + h, withOpacity(barBackground.get().getPacked()));
            if (filled > 0) context.fill(x, y, x + filled, y + h, withOpacity(barColor.get().getPacked()));
        }
    }

    // Resource pack textures

    private void renderTextures(DrawContext context, int cx, int cy, float progress, boolean inEditor) {
        int white = withOpacity(0xFFFFFFFF);

        // Vanilla: 15x15 sprite at (sw - 15) / 2, i.e. the center pixel is sprite pixel 7.
        context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, CROSSHAIR_TEXTURE, cx - 7, cy - 7, 15, 15, white);

        IndicatorState state = indicatorState(progress, inEditor);
        if (state == IndicatorState.Hidden) return;

        // Vanilla: x = sw / 2 - 8, y = sh / 2 - 7 + 16 (= center pixel -7 / +10 on even screen sizes).
        int x = cx - 7;
        int y = cy + indicatorOffset.get();

        if (state == IndicatorState.Full) {
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, INDICATOR_FULL_TEXTURE, x, y, 16, 16, white);
        } else {
            int filled = (int) (progress * 17f);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, INDICATOR_BACKGROUND_TEXTURE, x, y, 16, 4, white);
            if (filled > 0) {
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, INDICATOR_PROGRESS_TEXTURE, 16, 4, 0, 0, x, y, filled, 4, white);
            }
        }
    }

    // Color helpers

    private int withOpacity(int argb) {
        int a = (argb >>> 24) & 0xFF;
        a = (int) Math.round(a * opacity.get());
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static int lerpColor(int from, int to, float t) {
        t = Math.min(1f, Math.max(0f, t));
        int a = lerp((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, t);
        int r = lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, t);
        int g = lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, t);
        int b = lerp(from & 0xFF, to & 0xFF, t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    public enum Style {
        Cross("Cross"),
        Dot("Dot"),
        Circle("Circle"),
        CrossDot("Cross + Dot"),
        Custom("Custom");

        private final String title;

        Style(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum IndicatorMode {
        FollowVanilla("Follow vanilla option"),
        Always("Always"),
        Off("Off");

        private final String title;

        IndicatorMode(String title) {
            this.title = title;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
