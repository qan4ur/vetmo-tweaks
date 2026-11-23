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
public class CooldownsHud extends Module {
    public enum Layout { Horizontal, Vertical }
    public enum Style { Radial, Sweep, None }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPosition = settings.createGroup("Position");
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

    // Position

    private final Setting<Anchor> anchor = sgPosition.add(new EnumSetting.Builder<Anchor>()
        .name("anchor")
        .description("Screen corner/edge the HUD is attached to.")
        .defaultValue(Anchor.BottomCenter)
        .build()
    );

    private final Setting<Integer> offsetX = sgPosition.add(new IntSetting.Builder()
        .name("x-offset")
        .description("Horizontal offset from the anchor.")
        .defaultValue(-150)
        .sliderRange(-500, 500)
        .build()
    );

    private final Setting<Integer> offsetY = sgPosition.add(new IntSetting.Builder()
        .name("y-offset")
        .description("Vertical offset from the anchor.")
        .defaultValue(2)
        .sliderRange(-300, 300)
        .build()
    );

    private final Setting<Double> scale = sgPosition.add(new DoubleSetting.Builder()
        .name("scale")
        .description("HUD scale.")
        .defaultValue(1)
        .range(0.5, 3)
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
    }

    private final Map<Item, Track> tracks = new LinkedHashMap<>();
    private final List<Track> visible = new ArrayList<>();

    public CooldownsHud() {
        super(Categories.Visuals, "cooldowns-hud", "Shows active item cooldowns (pearl, chorus, totem, shield...).");
    }

    @Override
    public void onDeactivate() {
        tracks.clear();
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.options.hudHidden) return;

        ItemCooldownManager cooldowns = mc.player.getItemCooldownManager();
        long now = System.nanoTime();

        for (Track t : tracks.values()) t.alive = false;
        for (Item item : items.get()) {
            ItemStack stack = findStack(item);
            float p = cooldowns.getCooldownProgress(stack, event.tickDelta);
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
            t.alive = true;
        }

        float dt = (float) event.frameTime;
        tracks.values().removeIf(t -> {
            t.anim = approach(t.anim, t.alive ? 1 : 0, dt, t.alive ? 14 : 10);
            if (!t.alive) t.startNs = 0;
            return !t.alive && t.anim < 0.02f;
        });
        if (tracks.isEmpty()) return;

        visible.clear();
        visible.addAll(tracks.values());

        DrawContext ctx = event.drawContext;
        TextRenderer tr = mc.textRenderer;
        boolean horizontal = layout.get() == Layout.Horizontal;

        int pad = 3;
        int cellW = horizontal ? SLOT + 4 : SLOT + 4 + (seconds.get() ? tr.getWidth("00.0s") + 2 : 0);
        int cellH = horizontal ? SLOT + (seconds.get() ? 10 : 0) + (bar.get() ? 3 : 0) + 2 : SLOT + 2;
        if (!horizontal && bar.get() && !seconds.get()) cellW += 24;

        float span = 0;
        for (Track t : visible) span += easeOutCubic(t.anim) * (horizontal ? cellW : cellH);
        int w = Math.max(1, Math.round(horizontal ? span - 4 : cellW - 4)) + pad * 2;
        int h = Math.max(1, Math.round(horizontal ? cellH - 2 : span - 2)) + pad * 2;

        float s = scale.get().floatValue();
        float[] xy = anchored(ctx, anchor.get(), offsetX.get(), offsetY.get(), w * s, h * s);

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(xy[0], xy[1]);
        m.scale(s);

        float panelEase = 0;
        for (Track t : visible) panelEase = Math.max(panelEase, t.anim);
        if (background.get()) drawPanel(ctx, 0, 0, w, h, bgColor.get(), accentColor.get(), easeOutCubic(panelEase));

        float cursor = 0;
        for (Track t : visible) {
            float e = easeOutCubic(t.anim);
            float pop = t.alive ? easeOutBack(t.anim) : e;
            int alpha = (int) (255 * e);
            int cx = pad + Math.round(horizontal ? cursor : 0);
            int cy = pad + Math.round(horizontal ? 0 : cursor);

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

        m.popMatrix();
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
