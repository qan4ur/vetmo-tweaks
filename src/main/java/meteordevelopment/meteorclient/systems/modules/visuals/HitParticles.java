/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.random.Random;

/** Spawns client-only particles on the entity you hit. Nothing is sent to the server. */
public class HitParticles extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgCrits = settings.createGroup("Crits");
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Kind> kind = sgGeneral.add(new EnumSetting.Builder<Kind>()
        .name("particle")
        .description("Particle type to spawn.")
        .defaultValue(Kind.Dust)
        .build()
    );

    private final Setting<Boolean> normalHits = sgGeneral.add(new BoolSetting.Builder()
        .name("normal-hits")
        .description("Spawn particles on non-critical hits.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> amount = sgGeneral.add(new IntSetting.Builder()
        .name("amount")
        .description("Particles per normal hit.")
        .defaultValue(12)
        .range(1, 200)
        .sliderRange(1, 60)
        .visible(normalHits::get)
        .build()
    );

    private final Setting<Double> spread = sgGeneral.add(new DoubleSetting.Builder()
        .name("spread")
        .description("How far around the target's hitbox the particles appear.")
        .defaultValue(0.6)
        .range(0, 3)
        .sliderRange(0, 2)
        .build()
    );

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Initial particle velocity.")
        .defaultValue(0.15)
        .range(0, 2)
        .sliderRange(0, 1)
        .build()
    );

    private final Setting<Double> size = sgGeneral.add(new DoubleSetting.Builder()
        .name("size")
        .description("Dust particle size.")
        .defaultValue(1.0)
        .range(0.1, 4)
        .sliderRange(0.1, 4)
        .visible(() -> kind.get() == Kind.Dust)
        .build()
    );

    // Crits

    private final Setting<Boolean> crits = sgCrits.add(new BoolSetting.Builder()
        .name("crits")
        .description("Spawn particles on critical hits.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> critAmount = sgCrits.add(new IntSetting.Builder()
        .name("crit-amount")
        .description("Particles per critical hit.")
        .defaultValue(25)
        .range(1, 300)
        .sliderRange(1, 100)
        .visible(crits::get)
        .build()
    );

    private final Setting<Kind> critKind = sgCrits.add(new EnumSetting.Builder<Kind>()
        .name("crit-particle")
        .description("Particle type used for critical hits.")
        .defaultValue(Kind.Dust)
        .visible(crits::get)
        .build()
    );

    // Colors

    private final Setting<SettingColor> color = sgColors.add(new ColorSetting.Builder()
        .name("color")
        .description("Dust color on normal hits.")
        .defaultValue(new SettingColor(220, 20, 40))
        .build()
    );

    private final Setting<SettingColor> critColor = sgColors.add(new ColorSetting.Builder()
        .name("crit-color")
        .description("Dust color on critical hits.")
        .defaultValue(new SettingColor(255, 40, 40))
        .build()
    );

    private final Random random = Random.create();

    public HitParticles() {
        super(Categories.Visuals, "hit-particles", "Spawns custom client-side particles when you hit an entity.");
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (!(event.entity instanceof LivingEntity target) || !target.isAlive()) return;

        boolean crit = isCrit();
        if (crit && !crits.get()) return;
        if (!crit && !normalHits.get()) return;

        Kind k = crit ? critKind.get() : kind.get();
        int count = crit ? critAmount.get() : amount.get();
        ParticleEffect effect = k.effect((crit ? critColor : color).get().getPacked() & 0xFFFFFF, size.get().floatValue());

        double sp = spread.get();
        double halfW = target.getWidth() / 2.0 + sp * 0.5;
        double v = speed.get();

        for (int i = 0; i < count; i++) {
            double x = target.getX() + (random.nextDouble() * 2 - 1) * halfW;
            double y = target.getBodyY(random.nextDouble()) + (random.nextDouble() * 2 - 1) * sp * 0.5;
            double z = target.getZ() + (random.nextDouble() * 2 - 1) * halfW;

            double vx = (random.nextDouble() * 2 - 1) * v;
            double vy = random.nextDouble() * v;
            double vz = (random.nextDouble() * 2 - 1) * v;

            mc.world.addParticleClient(effect, x, y, z, vx, vy, vz);
        }
    }

    /**
     * Vanilla crit conditions (PlayerEntity#attack), evaluated client-side before the attack resets the cooldown:
     * charged > 0.9, falling, airborne, not climbing, not in water, not blind, not riding, not sprinting.
     */
    private boolean isCrit() {
        return mc.player.getAttackCooldownProgress(0.5f) > 0.9f
            && mc.player.fallDistance > 0
            && !mc.player.isOnGround()
            && !mc.player.isClimbing()
            && !mc.player.isTouchingWater()
            && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
            && !mc.player.hasVehicle()
            && !mc.player.isSprinting();
    }

    public enum Kind {
        Dust("Dust"),
        Crit("Crit"),
        EnchantedHit("Enchanted Hit"),
        Heart("Heart"),
        Flame("Flame"),
        SoulFire("Soul Fire");

        private final String title;

        Kind(String title) {
            this.title = title;
        }

        ParticleEffect effect(int rgb, float scale) {
            return switch (this) {
                case Dust -> new DustParticleEffect(rgb, scale);
                case Crit -> ParticleTypes.CRIT;
                case EnchantedHit -> ParticleTypes.ENCHANTED_HIT;
                case Heart -> ParticleTypes.HEART;
                case Flame -> ParticleTypes.FLAME;
                case SoulFire -> ParticleTypes.SOUL_FIRE_FLAME;
            };
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
