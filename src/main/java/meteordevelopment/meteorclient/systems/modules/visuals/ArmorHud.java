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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;

import static meteordevelopment.meteorclient.systems.modules.visuals.VetmoHudRender.*;

import java.util.ArrayList;
import java.util.List;

public class ArmorHud extends Module implements VetmoHudModule {
    public enum Layout { Horizontal, Vertical }
    public enum Display { Percent, Bar, Both }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Layout> layout = sgGeneral.add(new EnumSetting.Builder<Layout>()
        .name("layout")
        .description("Lay the pieces out in a row or a column.")
        .defaultValue(Layout.Horizontal)
        .build()
    );

    private final Setting<Display> display = sgGeneral.add(new EnumSetting.Builder<Display>()
        .name("durability")
        .description("How durability is shown.")
        .defaultValue(Display.Both)
        .build()
    );

    private final Setting<Boolean> mainHand = sgGeneral.add(new BoolSetting.Builder()
        .name("main-hand")
        .description("Also show the item in your main hand.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> offHand = sgGeneral.add(new BoolSetting.Builder()
        .name("off-hand")
        .description("Also show the item in your off hand.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> hideEmpty = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-empty")
        .description("Skip empty slots instead of drawing an empty frame.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> warnThreshold = sgGeneral.add(new IntSetting.Builder()
        .name("warn-threshold")
        .description("Pieces below this durability % blink red.")
        .defaultValue(20)
        .range(0, 100)
        .sliderRange(0, 100)
        .build()
    );

    private final Setting<Boolean> blink = sgGeneral.add(new BoolSetting.Builder()
        .name("blink")
        .description("Pulse low-durability pieces.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> blinkSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("blink-speed")
        .description("Pulses per second.")
        .defaultValue(2)
        .range(0.2, 8)
        .sliderRange(0.2, 8)
        .visible(blink::get)
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
        .description("Accent line and slot frames.")
        .defaultValue(new SettingColor(220, 20, 40))
        .build()
    );

    private final Setting<SettingColor> highColor = sgColors.add(new ColorSetting.Builder()
        .name("full-durability")
        .description("Text/bar color at full durability.")
        .defaultValue(new SettingColor(255, 170, 170))
        .build()
    );

    private final Setting<SettingColor> lowColor = sgColors.add(new ColorSetting.Builder()
        .name("low-durability")
        .description("Text/bar color at zero durability.")
        .defaultValue(new SettingColor(130, 0, 15))
        .build()
    );

    private final Setting<SettingColor> warnColor = sgColors.add(new ColorSetting.Builder()
        .name("warning")
        .description("Blink color for pieces below the threshold.")
        .defaultValue(new SettingColor(255, 40, 40, 120))
        .build()
    );

    private final Setting<SettingColor> textColor = sgColors.add(new ColorSetting.Builder()
        .name("text")
        .description("Color of item counts.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private static final EquipmentSlot[] ARMOR = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    private static final int SLOT = 20;
    private static final int PAD = 3;

    private final List<ItemStack> stacks = new ArrayList<>();
    private List<ItemStack> sampleStacks;
    private float appear;
    private long lastFrameNs;

    // layout of the current frame, computed in update()
    private boolean horizontal, showText, showBar;
    private int cellW, cellH, boxW, boxH;
    private float ease;

    public ArmorHud() {
        super(Categories.Visuals, "armor-hud", "Shows your armor and held items with durability.");
    }

    @Override
    public void onActivate() {
        appear = 0;
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
        return (int) Math.round(-60 * mc.getWindow().getScaleFactor());
    }

    @Override
    public boolean shouldRenderHud() {
        return mc.player != null && !mc.options.hudHidden && !stacks.isEmpty();
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
        double frameTime = lastFrameNs == 0 ? 0 : Math.min((now - lastFrameNs) / 1e9, 0.1);
        lastFrameNs = now;

        boolean editor = !Utils.canUpdate() || HudEditorScreen.isOpen();

        stacks.clear();
        if (mc.player != null) {
            for (EquipmentSlot slot : ARMOR) addStack(mc.player.getEquippedStack(slot));
            if (mainHand.get()) addStack(mc.player.getMainHandStack());
            if (offHand.get()) addStack(mc.player.getOffHandStack());
        }

        if (editor && stacks.stream().allMatch(ItemStack::isEmpty)) {
            stacks.clear();
            List<ItemStack> samples = samples();
            for (int i = 0; i < samples.size(); i++) {
                if (i == 4 && !mainHand.get()) continue;
                if (i == 5 && !offHand.get()) continue;
                stacks.add(samples.get(i));
            }
        }

        if (stacks.isEmpty()) {
            boxW = boxH = 1;
            return;
        }

        appear = approach(appear, 1, frameTime, 8);
        ease = easeOutCubic(appear);

        TextRenderer tr = mc.textRenderer;
        horizontal = layout.get() == Layout.Horizontal;
        showText = display.get() != Display.Bar;
        showBar = display.get() != Display.Percent;

        if (horizontal) {
            cellW = SLOT + 2;
            cellH = SLOT + (showText ? 10 : 0) + (showBar ? 3 : 0);
        } else {
            cellW = SLOT + 4 + (showText ? tr.getWidth("100%") + 2 : 0) + (showBar && !showText ? 24 : 0);
            cellH = SLOT + 2;
        }

        int count = stacks.size();
        boxW = (horizontal ? cellW * count - 2 : cellW) + PAD * 2;
        boxH = (horizontal ? cellH : cellH * count - 2) + PAD * 2;
    }

    private List<ItemStack> samples() {
        if (sampleStacks == null) {
            sampleStacks = new ArrayList<>();
            sampleStacks.add(damaged(Items.NETHERITE_HELMET, 0.92f));
            sampleStacks.add(damaged(Items.NETHERITE_CHESTPLATE, 0.74f));
            sampleStacks.add(damaged(Items.DIAMOND_LEGGINGS, 0.48f));
            sampleStacks.add(damaged(Items.DIAMOND_BOOTS, 0.15f));
            sampleStacks.add(damaged(Items.NETHERITE_SWORD, 0.86f));
            sampleStacks.add(new ItemStack(Items.TOTEM_OF_UNDYING));
        }
        return sampleStacks;
    }

    private static ItemStack damaged(Item item, float durability) {
        ItemStack stack = new ItemStack(item);
        if (stack.isDamageable()) stack.setDamage(Math.round(stack.getMaxDamage() * (1 - durability)));
        return stack;
    }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, boolean inEditor) {
        if (stacks.isEmpty()) return;

        TextRenderer tr = mc.textRenderer;
        int w = boxW, h = boxH;

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(0, (1 - ease) * 6);

        int alpha = (int) (255 * ease);
        if (background.get()) drawPanel(ctx, 0, 0, w, h, bgColor.get(), accentColor.get(), ease);

        float pulse = pulse(blinkSpeed.get());
        int count = stacks.size();

        for (int i = 0; i < count; i++) {
            ItemStack stack = stacks.get(i);
            int cx = PAD + (horizontal ? i * cellW : 0);
            int cy = PAD + (horizontal ? 0 : i * cellH);

            boolean damageable = !stack.isEmpty() && stack.isDamageable() && stack.getMaxDamage() > 0;
            float pct = damageable ? 1f - (float) stack.getDamage() / stack.getMaxDamage() : 1f;
            int pctInt = Math.round(pct * 100);
            boolean warn = damageable && pctInt < warnThreshold.get();

            // slot frame
            int frame = withAlpha(accentColor.get().getPacked(), (int) (60 * ease));
            ctx.fill(cx, cy, cx + SLOT, cy + SLOT, withAlpha(0xFF000000, (int) (70 * ease)));
            ctx.fill(cx, cy + SLOT - 1, cx + SLOT, cy + SLOT, frame);

            if (warn) {
                float k = blink.get() ? pulse : 1f;
                SettingColor wc = warnColor.get();
                ctx.fill(cx, cy, cx + SLOT, cy + SLOT, withAlpha(wc.getPacked(), (int) (wc.a * k * ease)));
                ctx.drawBorder(cx, cy, SLOT, SLOT, withAlpha(accentColor.get().getPacked(), (int) (255 * k * ease)));
            }

            if (!stack.isEmpty()) ctx.drawItem(stack, cx + 2, cy + 2);

            int color = lerpColor(lowColor.get().getPacked(), highColor.get().getPacked(), pct);
            if (warn && blink.get()) color = lerpColor(color, 0xFFFFFFFF, pulse * 0.6f);
            color = withAlpha(color, alpha);

            if (!damageable && stack.getCount() > 1) {
                String c = String.valueOf(stack.getCount());
                // drawn after the item so it stays on top
                ctx.drawText(tr, c, cx + SLOT - tr.getWidth(c), cy + SLOT - 8, withAlpha(textColor.get().getPacked(), alpha), shadow.get());
            }

            if (!damageable) continue;

            String text = pctInt + "%";
            if (horizontal) {
                int ty = cy + SLOT + 1;
                if (showText) {
                    ctx.drawText(tr, text, cx + (SLOT - tr.getWidth(text)) / 2 + 1, ty, color, shadow.get());
                    ty += 10;
                }
                if (showBar) drawBar(ctx, cx + 1, ty, SLOT - 2, pct, color, ease);
            } else {
                int tx = cx + SLOT + 4;
                if (showText) ctx.drawText(tr, text, tx, cy + (showBar ? 3 : 6), color, shadow.get());
                if (showBar) {
                    int bw = cellW - SLOT - 6;
                    drawBar(ctx, tx, cy + (showText ? 14 : 9), bw, pct, color, ease);
                }
            }
        }

        m.popMatrix();
    }

    private void addStack(ItemStack stack) {
        if (stack.isEmpty() && hideEmpty.get()) return;
        stacks.add(stack);
    }

    private void drawBar(DrawContext ctx, int x, int y, int w, float pct, int color, float ease) {
        ctx.fill(x, y, x + w, y + 2, withAlpha(0xFF000000, (int) (150 * ease)));
        int fw = Math.max(0, Math.round(w * MathHelper.clamp(pct, 0, 1)));
        if (fw > 0) ctx.fill(x, y, x + fw, y + 2, color);
    }
}
