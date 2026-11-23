/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.hud.XAnchor;
import meteordevelopment.meteorclient.systems.hud.YAnchor;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.visuals.hud.VetmoHudModule;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;

import java.util.Locale;

public class TargetHud extends Module implements VetmoHudModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgParts = settings.createGroup("Parts");
    private final SettingGroup sgHud = settings.createGroup("HUD");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Boolean> playersOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("players-only")
        .description("Only track players. Otherwise any living entity you attack is shown.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> crosshairTarget = sgGeneral.add(new BoolSetting.Builder()
        .name("crosshair-target")
        .description("Also pick up the entity under your crosshair.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> timeout = sgGeneral.add(new DoubleSetting.Builder()
        .name("timeout")
        .description("Seconds without combat before the target is dropped.")
        .defaultValue(4)
        .range(0.5, 30)
        .sliderRange(0.5, 15)
        .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Drop the target when it is further away than this.")
        .defaultValue(16)
        .range(3, 128)
        .sliderRange(3, 64)
        .build()
    );

    private final Setting<Boolean> previewInChat = sgGeneral.add(new BoolSetting.Builder()
        .name("preview-in-chat")
        .description("Shows a preview of yourself while chat is open.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showInScreens = sgGeneral.add(new BoolSetting.Builder()
        .name("show-in-screens")
        .description("Keep rendering while a non-chat screen (inventory, menus) is open.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> animationSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("animation-speed")
        .description("Speed of the health bar and fade animations.")
        .defaultValue(1)
        .range(0.2, 5)
        .sliderRange(0.2, 3)
        .build()
    );

    // Parts

    private final Setting<Boolean> showHead = sgParts.add(new BoolSetting.Builder()
        .name("head")
        .description("Shows the player's skin face.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showName = sgParts.add(new BoolSetting.Builder()
        .name("name")
        .description("Shows the target's name.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showBar = sgParts.add(new BoolSetting.Builder()
        .name("health-bar")
        .description("Shows an animated health bar.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showTrail = sgParts.add(new BoolSetting.Builder()
        .name("damage-trail")
        .description("A lagging bar that shows recently lost health.")
        .defaultValue(true)
        .visible(showBar::get)
        .build()
    );

    private final Setting<Boolean> showHealthText = sgParts.add(new BoolSetting.Builder()
        .name("health-number")
        .description("Shows the health as a number.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showAbsorption = sgParts.add(new BoolSetting.Builder()
        .name("absorption")
        .description("Shows absorption hearts on the bar and in the number.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showArmor = sgParts.add(new BoolSetting.Builder()
        .name("armor")
        .description("Shows the target's equipped armor.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> showHeld = sgParts.add(new BoolSetting.Builder()
        .name("held-item")
        .description("Shows the target's main hand item next to the armor.")
        .defaultValue(true)
        .visible(showArmor::get)
        .build()
    );

    private final Setting<Boolean> showDurability = sgParts.add(new BoolSetting.Builder()
        .name("durability")
        .description("Shows durability bars on the item icons.")
        .defaultValue(true)
        .visible(showArmor::get)
        .build()
    );

    private final Setting<Boolean> showDistance = sgParts.add(new BoolSetting.Builder()
        .name("distance")
        .description("Shows the distance to the target.")
        .defaultValue(true)
        .build()
    );

    // HUD

    private final Setting<Double> scale = sgHud.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Size of the panel. Mouse wheel over it in the HUD editor also changes it.")
        .defaultValue(1)
        .range(0.3, 4)
        .sliderRange(0.5, 3)
        .build()
    );

    // Colors

    private final Setting<SettingColor> backgroundColor = sgColors.add(new ColorSetting.Builder()
        .name("background")
        .description("Panel background.")
        .defaultValue(new SettingColor(18, 6, 8, 190))
        .build()
    );

    private final Setting<SettingColor> outlineColor = sgColors.add(new ColorSetting.Builder()
        .name("outline")
        .description("Panel outline.")
        .defaultValue(new SettingColor(220, 20, 40, 110))
        .build()
    );

    private final Setting<SettingColor> glowColor = sgColors.add(new ColorSetting.Builder()
        .name("glow")
        .description("Blood moon glow along the top edge.")
        .defaultValue(new SettingColor(255, 40, 40, 120))
        .build()
    );

    private final Setting<SettingColor> shadowColor = sgColors.add(new ColorSetting.Builder()
        .name("shadow")
        .description("Soft drop shadow.")
        .defaultValue(new SettingColor(0, 0, 0, 90))
        .build()
    );

    private final Setting<SettingColor> barStartColor = sgColors.add(new ColorSetting.Builder()
        .name("bar-start")
        .description("Left color of the health bar.")
        .defaultValue(new SettingColor(130, 0, 15))
        .build()
    );

    private final Setting<SettingColor> barEndColor = sgColors.add(new ColorSetting.Builder()
        .name("bar-end")
        .description("Right color of the health bar.")
        .defaultValue(new SettingColor(220, 20, 40))
        .build()
    );

    private final Setting<SettingColor> trailColor = sgColors.add(new ColorSetting.Builder()
        .name("trail")
        .description("Damage trail color.")
        .defaultValue(new SettingColor(255, 170, 170, 200))
        .build()
    );

    private final Setting<SettingColor> absorptionColor = sgColors.add(new ColorSetting.Builder()
        .name("absorption")
        .description("Absorption overlay color.")
        .defaultValue(new SettingColor(255, 120, 60, 220))
        .build()
    );

    private final Setting<SettingColor> barBackgroundColor = sgColors.add(new ColorSetting.Builder()
        .name("bar-background")
        .description("Empty part of the health bar.")
        .defaultValue(new SettingColor(40, 10, 14, 220))
        .build()
    );

    private final Setting<SettingColor> textColor = sgColors.add(new ColorSetting.Builder()
        .name("text")
        .description("Main text color.")
        .defaultValue(new SettingColor(255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> secondaryTextColor = sgColors.add(new ColorSetting.Builder()
        .name("secondary-text")
        .description("Secondary text color (health number, distance).")
        .defaultValue(new SettingColor(255, 170, 170))
        .build()
    );

    private final Setting<SettingColor> accentColor = sgColors.add(new ColorSetting.Builder()
        .name("accent")
        .description("Accent color (head frame, hurt flash).")
        .defaultValue(new SettingColor(220, 20, 40))
        .build()
    );

    // State

    private LivingEntity target;
    private long lastCombat;

    // Snapshot of the last shown entity (kept so the panel can fade out after the target is gone)
    private int shownId = Integer.MIN_VALUE;
    private boolean hasSnapshot;
    private String name = "";
    private float health, maxHealth = 20, absorption;
    private int hurtTime;
    private SkinTextures skin;
    private float distance;
    private final ItemStack[] items = new ItemStack[5];

    // Animation
    private double fade;
    private double dispHealth, trailHealth, dispAbsorption;
    private double lastHealth;
    private double sinceDamage;

    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public TargetHud() {
        super(Categories.Visuals, "target-hud", "Shows a panel with the health and gear of the player you are fighting.");
    }

    @Override
    public void onActivate() {
        target = null;
        hasSnapshot = false;
        shownId = Integer.MIN_VALUE;
        fade = 0;
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        onActivate();
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (isValid(event.entity)) {
            target = (LivingEntity) event.entity;
            lastCombat = System.currentTimeMillis();
        }
    }

    private boolean isValid(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == mc.player) return false;
        if (!living.isAlive()) return false;
        return !playersOnly.get() || entity instanceof PlayerEntity;
    }

    private void updateTarget() {
        if (mc.player == null) {
            target = null;
            return;
        }

        if (crosshairTarget.get() && isValid(mc.targetedEntity)) {
            if (target != mc.targetedEntity) target = (LivingEntity) mc.targetedEntity;
            lastCombat = System.currentTimeMillis();
        }

        if (target != null) {
            boolean gone = target.isRemoved() || target.isDead() || target.getWorld() != mc.world;
            boolean far = mc.player.distanceTo(target) > range.get();
            boolean timedOut = System.currentTimeMillis() - lastCombat > timeout.get() * 1000;
            if (gone || far || timedOut) target = null;
        }
    }

    private void snapshot(LivingEntity e) {
        if (e.getId() != shownId || !hasSnapshot) {
            shownId = e.getId();
            dispHealth = trailHealth = lastHealth = e.getHealth();
            dispAbsorption = e.getAbsorptionAmount();
            sinceDamage = 10;
        }
        hasSnapshot = true;
        name = e.getName().getString();
        health = Math.max(0, e.getHealth());
        maxHealth = Math.max(1, e.getMaxHealth());
        absorption = Math.max(0, e.getAbsorptionAmount());
        hurtTime = e.hurtTime;
        skin = e instanceof AbstractClientPlayerEntity p ? p.getSkinTextures() : null;
        distance = e == mc.player ? 0 : mc.player.distanceTo(e);
        for (int i = 0; i < 4; i++) items[i] = e.getEquippedStack(ARMOR_SLOTS[i]);
        items[4] = e.getEquippedStack(EquipmentSlot.MAINHAND);
    }

    // ---- animation (self-timed, the HUD element drives it every frame) ----

    private long lastFrame;

    private void update() {
        long now = System.nanoTime();
        double dt = lastFrame == 0 ? 0 : VetmoRender.dt((now - lastFrame) / 1e9);
        lastFrame = now;
        if (mc.player == null) return;
        double speed = animationSpeed.get();

        updateTarget();

        LivingEntity shown = target;
        if (shown == null && previewInChat.get() && VetmoRender.isChatOpen()) shown = mc.player;
        if (shown != null) snapshot(shown);

        fade = VetmoRender.approach(fade, shown != null ? 1 : 0, 10 * speed, dt);
        if (fade < 0.004) {
            fade = 0;
            if (shown == null) hasSnapshot = false;
        }

        // Health animation
        if (hasSnapshot) {
            if (health < lastHealth - 0.01) sinceDamage = 0;
            else sinceDamage += dt;
            lastHealth = health;

            dispHealth = VetmoRender.approach(dispHealth, health, 12 * speed, dt);
            dispAbsorption = VetmoRender.approach(dispAbsorption, absorption, 12 * speed, dt);
            if (trailHealth < dispHealth) trailHealth = dispHealth;
            else if (sinceDamage > 0.35 / speed) trailHealth = VetmoRender.approach(trailHealth, dispHealth, 4 * speed, dt);
        }
    }

    /** Fills the snapshot with yourself (or a dummy in the main menu) for the HUD editor preview. */
    private void previewSnapshot() {
        if (mc.player != null) {
            snapshot(mc.player);
            return;
        }
        shownId = Integer.MIN_VALUE;
        name = "Vetmo";
        health = 14;
        maxHealth = 20;
        absorption = 4;
        hurtTime = 0;
        skin = null;
        distance = 3.2f;
        java.util.Arrays.fill(items, ItemStack.EMPTY);
        dispHealth = lastHealth = health;
        trailHealth = 17;
        dispAbsorption = absorption;
    }

    // ---- HUD element ----

    @Override
    public boolean shouldRenderHud() {
        update();
        return hasSnapshot && fade > 0 && VetmoRender.shouldRender(showInScreens.get());
    }

    @Override
    public void renderHud(DrawContext ctx, float tickDelta, boolean inEditor) {
        update();
        boolean preview = inEditor && (!hasSnapshot || fade <= 0);
        if (preview) previewSnapshot();
        else if (!hasSnapshot || fade <= 0) return;

        render(ctx, preview ? 1 : fade);
    }

    @Override
    public double getHudWidth() {
        layout();
        return layoutW;
    }

    @Override
    public double getHudHeight() {
        layout();
        return layoutH;
    }

    @Override
    public Setting<Double> hudScale() {
        return scale;
    }

    @Override
    public int defaultHudX() {
        return 0;
    }

    @Override
    public int defaultHudY() {
        return (int) Math.round(60 * mc.getWindow().getScaleFactor());
    }

    @Override
    public XAnchor defaultHudXAnchor() {
        return XAnchor.Center;
    }

    @Override
    public YAnchor defaultHudYAnchor() {
        return YAnchor.Center;
    }

    // ---- layout (local, unscaled units) ----

    private static final int PAD = 6;
    private static final int HEAD_SIZE = 32;

    private int layoutW = 140, layoutH = 44;
    private int contentX, contentW, nameY, barY, bottomY, distW, itemCount, itemsW;
    private String shownName = "", distText = "", hpText = "";

    private void layout() {
        TextRenderer tr = mc.textRenderer;
        boolean head = showHead.get();
        boolean bottomRow = showHealthText.get() || showArmor.get();

        contentX = head ? PAD + HEAD_SIZE + 7 : PAD;
        distText = String.format(Locale.ROOT, "%.1fm", distance);
        distW = showDistance.get() ? tr.getWidth(distText) : 0;

        int nameMax = 120;
        shownName = name.isEmpty() && mc.player != null ? mc.player.getName().getString() : name;
        if (tr.getWidth(shownName) > nameMax) shownName = tr.trimToWidth(shownName, nameMax - tr.getWidth("...")) + "...";
        int nameW = showName.get() ? tr.getWidth(shownName) : 0;

        itemCount = showArmor.get() ? (showHeld.get() ? 5 : 4) : 0;
        itemsW = itemCount * 13;
        hpText = healthText();
        int hpW = showHealthText.get() ? tr.getWidth(hpText) + 6 : 0;

        contentW = Math.max(98, Math.max(nameW + (distW > 0 ? distW + 8 : 0), hpW + itemsW));
        layoutW = contentX + contentW + PAD;

        int cy = PAD;
        nameY = cy;
        if (showName.get() || showDistance.get()) cy += 12;
        barY = cy;
        if (showBar.get()) cy += 10;
        bottomY = cy;
        if (bottomRow) cy += 13;
        int h = Math.max(head ? PAD * 2 + HEAD_SIZE : 0, cy + PAD - 1);
        if (h <= PAD * 2) h = PAD * 2 + 8;
        layoutH = h;
    }

    private void render(DrawContext ctx, double fade) {
        TextRenderer tr = mc.textRenderer;
        layout();

        final int pad = PAD;
        final int headSize = HEAD_SIZE;
        boolean head = showHead.get();
        boolean bottomRow = showHealthText.get() || showArmor.get();
        int w = layoutW, h = layoutH;
        int contentX = this.contentX, contentW = this.contentW;
        int itemCount = this.itemCount, itemsW = this.itemsW;

        // ---- open animation, scaled around the box center ----
        double eased = VetmoRender.easeOutCubic(fade);
        double animScale = 0.88 + 0.12 * eased;

        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(w / 2f, h / 2f);
        m.scale((float) animScale, (float) animScale);
        m.translate(-w / 2f, -h / 2f);

        double a = fade;
        int bg = VetmoRender.withAlpha(backgroundColor.get().getPacked(), a);
        int outline = VetmoRender.withAlpha(outlineColor.get().getPacked(), a);
        int glow = VetmoRender.withAlpha(glowColor.get().getPacked(), a);
        int shadow = VetmoRender.withAlpha(shadowColor.get().getPacked(), a);
        int accent = VetmoRender.withAlpha(accentColor.get().getPacked(), a);
        int text = VetmoRender.withAlpha(textColor.get().getPacked(), a);
        int text2 = VetmoRender.withAlpha(secondaryTextColor.get().getPacked(), a);

        VetmoRender.panel(ctx, 0, 0, w, h, 4, bg, outline, shadow, 4, glow);

        double hurt = hurtTime / 10.0;

        // ---- head ----
        if (head) {
            int hx = pad, hy = (h - headSize) / 2;
            int frame = VetmoRender.lerpColor(hurt, VetmoRender.withAlpha(0xFF2A0A0E, a), accent);
            VetmoRender.roundedRect(ctx, hx - 1, hy - 1, hx + headSize + 1, hy + headSize + 1, 2, frame);
            if (skin != null) {
                int tint = VetmoRender.lerpColor(hurt * 0.85, 0xFFFFFFFF, 0xFFFF5050);
                PlayerSkinDrawer.draw(ctx, skin, hx, hy, headSize, VetmoRender.withAlpha(tint, a));
            } else {
                ctx.fill(hx, hy, hx + headSize, hy + headSize, VetmoRender.withAlpha(0xFF3A0A12, a));
                String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
                m.pushMatrix();
                m.translate(hx + headSize / 2f, hy + headSize / 2f);
                m.scale(2, 2);
                VetmoRender.text(ctx, letter, -tr.getWidth(letter) / 2, -4, text, true);
                m.popMatrix();
            }
        }

        // ---- name / distance ----
        if (showName.get()) VetmoRender.text(ctx, shownName, contentX, nameY, text, true);
        if (showDistance.get()) VetmoRender.text(ctx, distText, contentX + contentW - distW, nameY, text2, true);

        // ---- health bar ----
        if (showBar.get()) {
            int bx1 = contentX, bx2 = contentX + contentW;
            int by1 = barY, by2 = barY + 6;
            int bw = bx2 - bx1;

            VetmoRender.roundedRect(ctx, bx1, by1, bx2, by2, 2, VetmoRender.withAlpha(barBackgroundColor.get().getPacked(), a));

            double ratio = VetmoRender.clamp01(dispHealth / maxHealth);
            double trailRatio = VetmoRender.clamp01(trailHealth / maxHealth);

            if (showTrail.get() && trailRatio > ratio) {
                int tx2 = bx1 + (int) Math.round(bw * trailRatio);
                VetmoRender.roundedRect(ctx, bx1, by1, tx2, by2, 2, VetmoRender.withAlpha(trailColor.get().getPacked(), a));
            }

            int fx2 = bx1 + (int) Math.round(bw * ratio);
            if (fx2 > bx1) {
                int c1 = VetmoRender.withAlpha(barStartColor.get().getPacked(), a);
                int c2 = VetmoRender.lerpColor(ratio, c1, VetmoRender.withAlpha(barEndColor.get().getPacked(), a));
                VetmoRender.hGradient(ctx, bx1 + 1, by1, fx2 - 1, by2, c1, c2);
                ctx.fill(bx1, by1 + 1, bx1 + 1, by2 - 1, c1);
                if (fx2 - 1 > bx1) ctx.fill(fx2 - 1, by1 + 1, fx2, by2 - 1, c2);
                // glossy highlight on the top half
                ctx.fillGradient(bx1 + 1, by1, fx2 - 1, by1 + 3, VetmoRender.withAlpha(0x50FFFFFF, a), VetmoRender.withAlpha(0x00FFFFFF, a));
            }

            if (showAbsorption.get() && dispAbsorption > 0.05) {
                double absRatio = VetmoRender.clamp01(dispAbsorption / maxHealth);
                int ax2 = bx1 + (int) Math.round(bw * absRatio);
                ctx.fill(bx1, by2 - 2, ax2, by2, VetmoRender.withAlpha(absorptionColor.get().getPacked(), a));
            }
        }

        // ---- bottom row: health number + items ----
        if (bottomRow) {
            if (showHealthText.get()) {
                int hpColor = VetmoRender.lerpColor(hurt, text2, text);
                VetmoRender.text(ctx, hpText, contentX, bottomY + 2, hpColor, true);
            }

            // Items cannot be faded, so only draw them while mostly visible.
            if (itemCount > 0 && fade > 0.6) {
                int ix = contentX + contentW - itemsW;
                for (int i = 0; i < itemCount; i++) {
                    ItemStack stack = items[i];
                    if (stack == null || stack.isEmpty()) continue;
                    float px = ix + i * 13;
                    m.pushMatrix();
                    m.translate(px, bottomY);
                    m.scale(0.75f, 0.75f);
                    ctx.drawItem(stack, 0, 0);
                    if (showDurability.get()) ctx.drawStackOverlay(tr, stack, 0, 0);
                    m.popMatrix();
                }
            }
        }

        m.popMatrix();
    }

    private String healthText() {
        String hp = formatHp(health);
        if (showAbsorption.get() && absorption > 0) hp += " +" + formatHp(absorption);
        return hp + " ❤";
    }

    private static String formatHp(float v) {
        if (Math.abs(v - Math.round(v)) < 0.05f) return Integer.toString(Math.round(v));
        return String.format(Locale.ROOT, "%.1f", v);
    }
}
