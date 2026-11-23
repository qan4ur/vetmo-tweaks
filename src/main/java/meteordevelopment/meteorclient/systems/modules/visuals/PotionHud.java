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
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ColorHelper;
import org.joml.Matrix3x2fStack;

import java.util.*;

import static meteordevelopment.meteorclient.systems.modules.visuals.VetmoHudRender.*;

public class PotionHud extends Module {
    public enum Sort { Duration, Name, Harmful }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPosition = settings.createGroup("Position");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Boolean> hideVanilla = sgGeneral.add(new BoolSetting.Builder()
        .name("hide-vanilla-icons")
        .description("Hide the vanilla effect icons in the top-right corner.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showIcon = sgGeneral.add(new BoolSetting.Builder()
        .name("icons")
        .description("Draw the effect icon.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showLevel = sgGeneral.add(new BoolSetting.Builder()
        .name("level")
        .description("Show the effect level in roman numerals.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showAmbient = sgGeneral.add(new BoolSetting.Builder()
        .name("show-ambient")
        .description("Show ambient effects (beacons, conduits).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> respectHidden = sgGeneral.add(new BoolSetting.Builder()
        .name("respect-hidden")
        .description("Skip effects the server marked as not showing an icon.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Sort> sort = sgGeneral.add(new EnumSetting.Builder<Sort>()
        .name("sort")
        .description("How effects are ordered.")
        .defaultValue(Sort.Duration)
        .build()
    );

    private final Setting<Integer> blinkBelow = sgGeneral.add(new IntSetting.Builder()
        .name("blink-below")
        .description("Blink effects with fewer seconds left than this.")
        .defaultValue(10)
        .range(0, 120)
        .sliderRange(0, 60)
        .build()
    );

    private final Setting<Boolean> effectColorBar = sgGeneral.add(new BoolSetting.Builder()
        .name("effect-color-bar")
        .description("Thin bar on the side of each row in the effect's own color.")
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
        .description("Screen corner/edge the list is attached to.")
        .defaultValue(Anchor.TopRight)
        .build()
    );

    private final Setting<Integer> offsetX = sgPosition.add(new IntSetting.Builder()
        .name("x-offset")
        .description("Horizontal offset from the anchor.")
        .defaultValue(4)
        .sliderRange(-500, 500)
        .build()
    );

    private final Setting<Integer> offsetY = sgPosition.add(new IntSetting.Builder()
        .name("y-offset")
        .description("Vertical offset from the anchor.")
        .defaultValue(4)
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
        .description("Accent line color.")
        .defaultValue(new SettingColor(220, 20, 40))
        .build()
    );

    private final Setting<SettingColor> nameColor = sgColors.add(new ColorSetting.Builder()
        .name("name")
        .description("Effect name color.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> harmfulColor = sgColors.add(new ColorSetting.Builder()
        .name("harmful-name")
        .description("Name color for harmful effects.")
        .defaultValue(new SettingColor(255, 90, 90))
        .build()
    );

    private final Setting<SettingColor> timerColor = sgColors.add(new ColorSetting.Builder()
        .name("timer")
        .description("Timer color.")
        .defaultValue(new SettingColor(255, 170, 170))
        .build()
    );

    private final Setting<SettingColor> blinkColor = sgColors.add(new ColorSetting.Builder()
        .name("blink")
        .description("Color effects blink to when running out.")
        .defaultValue(new SettingColor(255, 40, 40, 120))
        .build()
    );

    private static final int ROW_H = 22;

    private static final class Row {
        StatusEffectInstance instance;
        float progress;
        boolean alive;
    }

    private final Map<RegistryEntry<StatusEffect>, Row> rows = new LinkedHashMap<>();
    private final List<Row> order = new ArrayList<>();
    private float animWidth = -1;

    public PotionHud() {
        super(Categories.Visuals, "potion-hud", "Lists your active status effects with timers.");
    }

    @Override
    public void onDeactivate() {
        rows.clear();
        animWidth = -1;
    }

    /** Used by VetmoInGameHudMixin. */
    public boolean shouldHideVanilla() {
        return isActive() && hideVanilla.get();
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null || mc.options.hudHidden) return;

        // sync rows with active effects
        for (Row row : rows.values()) row.alive = false;
        for (StatusEffectInstance inst : mc.player.getStatusEffects()) {
            if (!showAmbient.get() && inst.isAmbient()) continue;
            if (respectHidden.get() && !inst.shouldShowIcon()) continue;
            Row row = rows.computeIfAbsent(inst.getEffectType(), k -> new Row());
            row.instance = inst;
            row.alive = true;
        }

        float dt = (float) event.frameTime;
        rows.values().removeIf(row -> {
            row.progress = approach(row.progress, row.alive ? 1 : 0, dt, 12);
            if (row.alive && row.progress > 0.995f) row.progress = 1;
            return !row.alive && row.progress < 0.02f;
        });
        if (rows.isEmpty()) {
            animWidth = -1;
            return;
        }

        order.clear();
        order.addAll(rows.values());
        Comparator<Row> cmp = switch (sort.get()) {
            case Duration -> Comparator.comparingInt(r -> r.instance.isInfinite() ? Integer.MAX_VALUE : r.instance.getDuration());
            case Name -> Comparator.comparing(r -> r.instance.getEffectType().value().getName().getString());
            case Harmful -> Comparator.<Row>comparingInt(r -> r.instance.getEffectType().value().isBeneficial() ? 1 : 0)
                .thenComparingInt(r -> r.instance.isInfinite() ? Integer.MAX_VALUE : r.instance.getDuration());
        };
        order.sort(cmp);

        DrawContext ctx = event.drawContext;
        TextRenderer tr = mc.textRenderer;

        // measure
        int iconW = showIcon.get() ? 20 : 0;
        int textW = 0;
        float totalH = 0;
        for (Row row : order) {
            int nameW = tr.getWidth(label(row.instance));
            int timeW = tr.getWidth(timer(row.instance));
            textW = Math.max(textW, Math.max(nameW, timeW));
            totalH += ROW_H * easeOutCubic(row.progress);
        }
        int pad = 3;
        float targetW = pad * 2 + iconW + textW + 6;
        animWidth = animWidth < 0 ? targetW : approach(animWidth, targetW, dt, 14);
        int w = Math.round(animWidth);
        int h = Math.round(totalH) + pad * 2;

        float s = scale.get().floatValue();
        float[] xy = anchored(ctx, anchor.get(), offsetX.get(), offsetY.get(), w * s, h * s);
        boolean right = isRightSide(anchor.get());

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(xy[0], xy[1]);
        m.scale(s);

        drawPanel(ctx, 0, 0, w, h, bgColor.get(), accentColor.get(), 1);

        float pulse = pulse(1.6);
        float y = pad;
        for (Row row : order) {
            float e = easeOutCubic(row.progress);
            StatusEffectInstance inst = row.instance;
            int ry = Math.round(y);
            int alpha = (int) (255 * e);
            float slide = (1 - e) * 16 * (right ? 1 : -1);

            boolean low = !inst.isInfinite() && inst.getDuration() < blinkBelow.get() * 20;
            float k = low ? pulse : 0;

            m.pushMatrix();
            m.translate(slide, 0);

            if (low) {
                SettingColor bc = blinkColor.get();
                ctx.fill(pad, ry, w - pad, ry + ROW_H - 2, withAlpha(bc.getPacked(), (int) (bc.a * k * e)));
            }

            if (effectColorBar.get()) {
                int ec = withAlpha(inst.getEffectType().value().getColor(), alpha);
                ctx.fill(pad, ry + 2, pad + 1, ry + ROW_H - 4, ec);
            }

            int tx = pad + 3;
            if (showIcon.get()) {
                float iconAlpha = e * (low ? 0.45f + 0.55f * (1 - k) : 1f);
                ctx.drawGuiTexture(RenderPipelines.GUI_TEXTURED, InGameHud.getEffectTexture(inst.getEffectType()), tx, ry + 1, 18, 18, ColorHelper.getWhite(iconAlpha));
                tx += iconW;
            }

            int nc = inst.getEffectType().value().isBeneficial() ? nameColor.get().getPacked() : harmfulColor.get().getPacked();
            ctx.drawText(tr, label(inst), tx, ry + 1, withAlpha(nc, alpha), shadow.get());

            int tc = timerColor.get().getPacked();
            if (low) tc = lerpColor(tc, accentColor.get().getPacked() | 0xFF000000, k);
            ctx.drawText(tr, timer(inst), tx, ry + 11, withAlpha(tc, alpha), shadow.get());

            m.popMatrix();
            y += ROW_H * e;
        }

        m.popMatrix();
    }

    private String label(StatusEffectInstance inst) {
        String name = inst.getEffectType().value().getName().getString();
        if (!showLevel.get() || inst.getAmplifier() <= 0) return name;
        return name + " " + roman(inst.getAmplifier() + 1);
    }

    private static String timer(StatusEffectInstance inst) {
        if (inst.isInfinite()) return "∞";
        int total = Math.max(0, inst.getDuration() / 20);
        int min = total / 60, sec = total % 60;
        if (min >= 60) return String.format("%d:%02d:%02d", min / 60, min % 60, sec);
        return String.format("%d:%02d", min, sec);
    }

    private static final int[] ROMAN_VAL = { 10, 9, 5, 4, 1 };
    private static final String[] ROMAN_SYM = { "X", "IX", "V", "IV", "I" };

    private static String roman(int n) {
        if (n <= 0 || n > 39) return String.valueOf(n);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ROMAN_VAL.length; i++) {
            while (n >= ROMAN_VAL[i]) {
                sb.append(ROMAN_SYM[i]);
                n -= ROMAN_VAL[i];
            }
        }
        return sb.toString();
    }
}
