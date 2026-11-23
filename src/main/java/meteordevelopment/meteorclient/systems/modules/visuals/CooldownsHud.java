/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.XAnchor;
import meteordevelopment.meteorclient.systems.hud.YAnchor;
import meteordevelopment.meteorclient.systems.hud.screens.HudEditorScreen;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.visuals.hud.VetmoHudModule;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;

import java.util.*;

import static meteordevelopment.meteorclient.systems.modules.visuals.VetmoHudRender.*;

/**
 * Shows item cooldowns. Reads only {@link ItemCooldownManager#getCooldownProgress(ItemStack, float)}, i.e. exactly what the vanilla hotbar overlay shows.
 */
public class CooldownsHud extends Module implements VetmoHudModule {
    public enum Layout { Horizontal, Vertical }
    public enum Style { Radial, Sweep, None }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
        .name("items")
        .description("Items whose cooldowns are shown.")
        .defaultValue(Items.ENDER_PEARL, Items.CHORUS_FRUIT, Items.TOTEM_OF_UNDYING, Items.SHIELD, Items.WIND_CHARGE)
        .build()
    );

    private final Setting<Layout> layout = sgGeneral.add(new EnumSetting.Builder<Layout>()
        .name("layout")
        .description("Lay the cooldowns out in a row or a column.")
        .defaultValue(Layout.Horizontal)
        .build()
    );

    private final Setting<Style> style = sgGeneral.add(new EnumSetting.Builder<Style>()
        .name("overlay")
        .description("Progress overlay drawn over the item icon.")
        .defaultValue(Style.Radial)
        .build()
    );

    private final Setting<Boolean> bar = sgGeneral.add(new BoolSetting.Builder()
        .name("bar")
        .description("Draw a progress bar under each item.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> seconds = sgGeneral.add(new BoolSetting.Builder()
        .name("seconds")
        .description("Show an estimate of the seconds left.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> background = sgGeneral.add(new BoolSetting.Builder()
        .name("background")
        .description("Draw a panel behind the HUD.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("text-shadow")
        .description("Draw text with a shadow.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
        .name("scale")
        .description("HUD scale (mouse wheel over the element in the HUD editor).")
        .defaultValue(1)
        .min(0.3)
        .max(4)
        .sliderRange(0.5, 3)
        .build()
    );

    // Colors

    private final Setting<SettingColor> bgColor = sgColors.add(new ColorSetting.Builder()
        .name("background")
        .description("Panel background.")
        .defaultValue(new SettingColor(18, 6, 8, 190))
        .build()
    );

    private final Setting<SettingColor> accentColor = sgColors.add(new ColorSetting.Builder()
        .name("accent")
        .description("Accent line and progress bar.")
        .defaultValue(new SettingColor(220, 20, 40))
        .build()
    );

    private final Setting<SettingColor> overlayColor = sgColors.add(new ColorSetting.Builder()
        .name("overlay")
        .description("Color of the remaining-cooldown overlay on the icon.")
        .defaultValue(new SettingColor(130, 0, 15, 150))
        .build()
    );

    private final Setting<SettingColor> textColor = sgColors.add(new ColorSetting.Builder()
        .name("text")
        .description("Seconds text color.")
        .defaultValue(new SettingColor(255, 170, 170))
        .build()
    );

    private static final int SLOT = 20;

    private static final class Track {
        Item item;
        ItemStack stack;
        float progress;
        float anim;
        boolean alive;
        // for the seconds estimate
        float startProgress;
        long startNs;
        float lastProgress;
        // editor preview: fixed length in seconds instead of the estimate
        double sampleSeconds;
    }

    private static final int PAD = 3;

    private final Map<Item, Track> tracks = new LinkedHashMap<>();
    private final List<Track> visible = new ArrayList<>();
    private long lastFrameNs;

    // layout of the current frame, computed in update()
    private boolean horizontal;
    private int cellW, cellH, boxW = 1, boxH = 1;
    private long frameNs;

    public CooldownsHud() {
        super(Categories.Visuals, "cooldowns-hud", "Shows active item cooldowns (pearl, chorus, totem, shield...).");
    }

    @Override
    public void onDeactivate() {
        tracks.clear();
        visible.clear();
        lastFrameNs = 0;
    }

    // HUD element

    @Override
    public Setting<Double> hudScale() {
        return scale;
    }

    @Override
    public XAnchor defaultHudXAnchor() {
        return XAnchor.Center;
    }

    @Override
    public YAnchor defaultHudYAnchor() {
        return YAnchor.Bottom;
    }

    @Override
    public int defaultHudX() {
        return 0;
    }

    @Override
    public int defaultHudY() {
        return (int) Math.round(-85 * mc.getWindow().getScaleFactor());
    }

    @Override
    public boolean shouldRenderHud() {
        return mc.player != null && !mc.options.hudHidden && !visible.isEmpty();
    }

    /** Called first by the HUD element every frame, so the per-frame state is updated here. */
    @Override
    public double getHudWidth() {
        update();
        return boxW;
    }

    @Override
    public double getHudHeight() {
        return boxH;
    }

    private void update() {
        long now = System.nanoTime();
        float dt = lastFrameNs == 0 ? 0 : (float) Math.min((now - lastFrameNs) / 1e9, 0.1);
        lastFrameNs = now;
        frameNs = now;

        boolean editor = !Utils.canUpdate() || HudEditorScreen.isOpen();
        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);

        for (Track t : tracks.values()) t.alive = false;
        boolean any = false;
        if (mc.player != null) {
            ItemCooldownManager cooldowns = mc.player.getItemCooldownManager();
            for (Item item : items.get()) {
                ItemStack stack = findStack(item);
                float p = cooldowns.getCooldownProgress(stack, tickDelta);
                if (p <= 0) continue;

                Track t = tracks.computeIfAbsent(item, k -> new Track());
                if (t.item == null || p > t.lastProgress + 0.02f || t.startNs == 0) {
                    // new (or restarted) cooldown
                    t.startProgress = p;
                    t.startNs = now;
                }
                t.item = item;
                t.stack = stack;
                t.progress = p;
                t.lastProgress = p;
                t.sampleSeconds = 0;
                t.alive = true;
                any = true;
            }
        }

        // preview in the HUD editor when nothing is on cooldown: a looping pearl and chorus cooldown
        if (editor && !any) {
            double time = now / 1e9;
            sample(Items.ENDER_PEARL, 3.0, time);
            sample(Items.CHORUS_FRUIT, 4.0, time + 1.3);
        }

        tracks.values().removeIf(t -> {
            t.anim = approach(t.anim, t.alive ? 1 : 0, dt, t.alive ? 14 : 10);
            if (!t.alive) t.startNs = 0;
            return !t.alive && t.anim < 0.02f;
        });

        visible.clear();
        if (tracks.isEmpty()) {
            boxW = boxH = 1;
            return;
        }
        visible.addAll(tracks.values());

        TextRenderer tr = mc.textRenderer;
        horizontal = layout.get() == Layout.Horizontal;

        cellW = horizontal ? SLOT + 4 : SLOT + 4 + (seconds.get() ? tr.getWidth("00.0s") + 2 : 0);
        cellH = horizontal ? SLOT + (seconds.get() ? 10 : 0) + (bar.get() ? 3 : 0) + 2 : SLOT + 2;
        if (!horizontal && bar.get() && !seconds.get()) cellW += 24;

        float span = 0;
        for (Track t : visible) span += easeOutCubic(t.anim) * (horizontal ? cellW : cellH);
        boxW = Math.max(1, Math.round(horizontal ? span - 4 : cellW - 4)) + PAD * 2;
        boxH = Math.max(1, Math.round(horizontal ? cellH - 2 : span - 2)) + PAD * 2;
    }

    private void sample(Item item, double period, double time) {
        Track t = tracks.computeIfAbsent(item, k -> new Track());
        t.item = item;
        t.stack = item.getDefaultStack();
        t.progress = (float) (1 - (time % period) / period);
        t.lastProgress = t.progress;
        t.sampleSeconds = period;
        t.alive = true;
    }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, boolean inEditor) {
        if (visible.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;
        int w = boxW, h = boxH;
        long now = frameNs;

        Matrix3x2fStack m = ctx.getMatrices();

        float panelEase = 0;
        for (Track t : visible) panelEase = Math.max(panelEase, t.anim);
        if (background.get()) drawPanel(ctx, 0, 0, w, h, bgColor.get(), accentColor.get(), easeOutCubic(panelEase));

        float cursor = 0;
        for (Track t : visible) {
            float e = easeOutCubic(t.anim);
            float pop = t.alive ? easeOutBack(t.anim) : e;
            int alpha = (int) (255 * e);
            int cx = PAD + Math.round(horizontal ? cursor : 0);
            int cy = PAD + Math.round(horizontal ? 0 : cursor);

            m.pushMatrix();
            // scale the slot around its centre for the pop-in
            m.translate(cx + SLOT / 2f, cy + SLOT / 2f);
            m.scale(Math.max(0.01f, pop));
            m.translate(-SLOT / 2f, -SLOT / 2f);

            ctx.fill(0, 0, SLOT, SLOT, withAlpha(0xFF000000, (int) (80 * e)));
            ctx.drawItem(t.stack, 2, 2);
            drawOverlay(ctx, t.progress, fade(overlayColor.get().getPacked(), e));
            ctx.fill(0, SLOT - 1, SLOT, SLOT, withAlpha(accentColor.get().getPacked(), (int) (90 * e)));
            m.popMatrix();

            String secText = seconds.get() ? secondsLeft(t, now) : null;
            int tc = withAlpha(textColor.get().getPacked(), alpha);
            int ac = withAlpha(accentColor.get().getPacked(), alpha);

            if (horizontal) {
                int ty = cy + SLOT + 1;
                if (secText != null) {
                    ctx.drawText(tr, secText, cx + (SLOT - tr.getWidth(secText)) / 2 + 1, ty, tc, shadow.get());
                    ty += 10;
                }
                if (bar.get()) drawBar(ctx, cx + 1, ty, SLOT - 2, t.progress, ac, e);
            } else {
                int tx = cx + SLOT + 4;
                if (secText != null) ctx.drawText(tr, secText, tx, cy + (bar.get() ? 3 : 6), tc, shadow.get());
                if (bar.get()) drawBar(ctx, tx, cy + (secText != null ? 14 : 9), cellW - SLOT - 8, t.progress, ac, e);
            }

            cursor += e * (horizontal ? cellW : cellH);
        }
    }

    private ItemStack findStack(Item item) {
        // prefer a real stack (servers can give items custom cooldown groups), fall back to the default stack
        PlayerInventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isOf(item)) return stack;
        }
        return item.getDefaultStack();
    }

    private void drawOverlay(DrawContext ctx, float progress, int color) {
        progress = MathHelper.clamp(progress, 0, 1);
        switch (style.get()) {
            case Sweep -> {
                int top = Math.round(SLOT * (1 - progress));
                ctx.fill(0, top, SLOT, SLOT, color);
            }
            case Radial -> {
                // clockwise pie from 12 o'clock; the covered part is the remaining cooldown. One fill per run of pixels per row.
                double startAngle = (1 - progress) * Math.PI * 2;
                for (int py = 0; py < SLOT; py++) {
                    int runStart = -1;
                    for (int px = 0; px <= SLOT; px++) {
                        boolean covered = false;
                        if (px < SLOT) {
                            double dx = px + 0.5 - SLOT / 2.0, dy = py + 0.5 - SLOT / 2.0;
                            double a = Math.atan2(dx, -dy);
                            if (a < 0) a += Math.PI * 2;
                            covered = a >= startAngle;
                        }
                        if (covered && runStart < 0) runStart = px;
                        else if (!covered && runStart >= 0) {
                            ctx.fill(runStart, py, px, py + 1, color);
                            runStart = -1;
                        }
                    }
                }
            }
            case None -> {
            }
        }
    }

    private void drawBar(DrawContext ctx, int x, int y, int w, float pct, int color, float ease) {
        ctx.fill(x, y, x + w, y + 2, withAlpha(0xFF000000, (int) (150 * ease)));
        int fw = Math.round(w * MathHelper.clamp(pct, 0, 1));
        if (fw > 0) ctx.fill(x, y, x + fw, y + 2, color);
    }

    private static String secondsLeft(Track t, long now) {
        if (!t.alive) return "0.0s";
        if (t.sampleSeconds > 0) return String.format(Locale.ROOT, "%.1fs", t.progress * t.sampleSeconds);
        double elapsed = (now - t.startNs) / 1e9;
        double used = t.startProgress - t.progress;
        double remaining;
        if (elapsed > 0.1 && used > 0.005) {
            double total = elapsed / used;
            remaining = t.progress * total;
        } else {
            Double total = defaultSeconds(t.item);
            if (total == null) return "...";
            remaining = t.progress * total;
        }
        return String.format(Locale.ROOT, "%.1fs", Math.max(0, remaining));
    }

    /** Vanilla cooldown lengths, used only until the rate-based estimate kicks in (~0.1s). */
    private static Double defaultSeconds(Item item) {
        if (item == Items.ENDER_PEARL || item == Items.CHORUS_FRUIT) return 1.0;
        if (item == Items.SHIELD) return 5.0;
        if (item == Items.WIND_CHARGE) return 0.5;
        return null;
    }
}
