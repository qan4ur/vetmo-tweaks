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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix3x2fStack;

import static meteordevelopment.meteorclient.systems.modules.visuals.VetmoHudRender.*;

import java.util.ArrayList;
import java.util.List;

public class ArmorHud extends Module {
    public enum Layout { Horizontal, Vertical }
    public enum Display { Percent, Bar, Both }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPosition = settings.createGroup("Position");
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
        .defaultValue(150)
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

    private final List<ItemStack> stacks = new ArrayList<>();
    private float appear;

    public ArmorHud() {
        super(Categories.Visuals, "armor-hud", "Shows your armor and held items with durability.");
    }

    @Override
    public void onActivate() {
        appear = 0;
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.options.hudHidden) return;

        stacks.clear();
        for (EquipmentSlot slot : ARMOR) addStack(mc.player.getEquippedStack(slot));
        if (mainHand.get()) addStack(mc.player.getMainHandStack());
        if (offHand.get()) addStack(mc.player.getOffHandStack());
        if (stacks.isEmpty()) return;

        appear = approach(appear, 1, event.frameTime, 8);
        float ease = easeOutCubic(appear);

        DrawContext ctx = event.drawContext;
        TextRenderer tr = mc.textRenderer;
        boolean horizontal = layout.get() == Layout.Horizontal;
        boolean showText = display.get() != Display.Bar;
        boolean showBar = display.get() != Display.Percent;

        int pad = 3;
        int cellW, cellH;
        if (horizontal) {
            cellW = SLOT + 2;
            cellH = SLOT + (showText ? 10 : 0) + (showBar ? 3 : 0);
        } else {
            cellW = SLOT + 4 + (showText ? tr.getWidth("100%") + 2 : 0) + (showBar && !showText ? 24 : 0);
            cellH = SLOT + 2;
        }

        int count = stacks.size();
        int w = (horizontal ? cellW * count - 2 : cellW) + pad * 2;
        int h = (horizontal ? cellH : cellH * count - 2) + pad * 2;

        float s = scale.get().floatValue();
        float[] xy = anchored(ctx, anchor.get(), offsetX.get(), offsetY.get(), w * s, h * s);

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(xy[0], xy[1] + (1 - ease) * 6);
        m.scale(s);

        int alpha = (int) (255 * ease);
        if (background.get()) drawPanel(ctx, 0, 0, w, h, bgColor.get(), accentColor.get(), ease);

        float pulse = pulse(blinkSpeed.get());

        for (int i = 0; i < count; i++) {
            ItemStack stack = stacks.get(i);
            int cx = pad + (horizontal ? i * cellW : 0);
            int cy = pad + (horizontal ? 0 : i * cellH);

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
