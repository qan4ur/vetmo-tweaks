/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import meteordevelopment.meteorclient.events.entity.player.AttackEntityEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Plays a purely client-side effect when an entity you recently hit dies.
 * Death is detected from the health the server already syncs (isDead) and the vanilla death status (EntityStatus 3).
 */
public class KillEffect extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgLightning = settings.createGroup("Lightning");
    private final SettingGroup sgParticles = settings.createGroup("Particles");
    private final SettingGroup sgSound = settings.createGroup("Sound");

    // General

    private final Setting<Boolean> playersOnly = sgGeneral.add(new BoolSetting.Builder()
        .name("players-only")
        .description("Only trigger on players (otherwise any living entity).")
        .defaultValue(true)
        .build()
    );

    private final Setting<Double> attackWindow = sgGeneral.add(new DoubleSetting.Builder()
        .name("attack-window")
        .description("Seconds after your last hit during which a death counts as your kill.")
        .defaultValue(5)
        .range(0.5, 30)
        .sliderRange(1, 15)
        .build()
    );

    // Lightning

    private final Setting<Boolean> lightning = sgLightning.add(new BoolSetting.Builder()
        .name("lightning")
        .description("Strikes a cosmetic, client-only lightning bolt at the victim.")
        .defaultValue(true)
        .build()
    );

    // Particles

    private final Setting<Boolean> particles = sgParticles.add(new BoolSetting.Builder()
        .name("particles")
        .description("Bursts a ring of dust particles around the victim.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> particleColor = sgParticles.add(new ColorSetting.Builder()
        .name("particle-color")
        .description("Color of the dust burst.")
        .defaultValue(new SettingColor(220, 20, 40))
        .visible(particles::get)
        .build()
    );

    private final Setting<Integer> particleCount = sgParticles.add(new IntSetting.Builder()
        .name("particles-per-ring")
        .description("Particles in each ring.")
        .defaultValue(32)
        .range(4, 200)
        .sliderRange(8, 96)
        .visible(particles::get)
        .build()
    );

    private final Setting<Integer> rings = sgParticles.add(new IntSetting.Builder()
        .name("rings")
        .description("Number of stacked rings along the victim's height.")
        .defaultValue(4)
        .range(1, 16)
        .sliderRange(1, 8)
        .visible(particles::get)
        .build()
    );

    private final Setting<Double> radius = sgParticles.add(new DoubleSetting.Builder()
        .name("radius")
        .description("Starting radius of the rings.")
        .defaultValue(0.6)
        .range(0, 4)
        .sliderRange(0, 2)
        .visible(particles::get)
        .build()
    );

    private final Setting<Double> burstSpeed = sgParticles.add(new DoubleSetting.Builder()
        .name("burst-speed")
        .description("Outward velocity of the burst.")
        .defaultValue(1.5)
        .range(0, 6)
        .sliderRange(0, 4)
        .visible(particles::get)
        .build()
    );

    private final Setting<Double> particleSize = sgParticles.add(new DoubleSetting.Builder()
        .name("particle-size")
        .description("Size of the dust particles.")
        .defaultValue(1.5)
        .range(0.1, 4)
        .sliderRange(0.1, 4)
        .visible(particles::get)
        .build()
    );

    // Sound

    private final Setting<Boolean> sound = sgSound.add(new BoolSetting.Builder()
        .name("sound")
        .description("Plays a sound only you can hear.")
        .defaultValue(true)
        .build()
    );

    private final Setting<KillSound> soundType = sgSound.add(new EnumSetting.Builder<KillSound>()
        .name("sound-type")
        .description("Which sound to play.")
        .defaultValue(KillSound.LevelUp)
        .visible(sound::get)
        .build()
    );

    private final Setting<Double> volume = sgSound.add(new DoubleSetting.Builder()
        .name("volume")
        .description("Sound volume.")
        .defaultValue(1)
        .range(0, 2)
        .sliderRange(0, 2)
        .visible(sound::get)
        .build()
    );

    private final Setting<Double> pitch = sgSound.add(new DoubleSetting.Builder()
        .name("pitch")
        .description("Sound pitch.")
        .defaultValue(1)
        .range(0.5, 2)
        .sliderRange(0.5, 2)
        .visible(sound::get)
        .build()
    );

    /** entity id -> time (ms) of my last hit on it. Only touched on the client thread. */
    private final Int2LongMap attacked = new Int2LongOpenHashMap();
    private final List<LightningEntity> bolts = new ArrayList<>();
    private final List<Integer> boltAges = new ArrayList<>();
    /** Negative ids so local bolts never collide with server entity ids. */
    private int nextBoltId = -1_000_000;

    public KillEffect() {
        super(Categories.Visuals, "kill-effect", "Plays a client-side effect when a player you hit dies.");
    }

    @Override
    public void onDeactivate() {
        attacked.clear();
        clearBolts();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        attacked.clear();
        bolts.clear();
        boltAges.clear();
    }

    @EventHandler
    private void onAttack(AttackEntityEvent event) {
        Entity e = event.entity;
        if (mc.player == null || e == mc.player || !isValidTarget(e)) return;
        attacked.put(e.getId(), System.currentTimeMillis());
    }

    // Netty thread: only read the status here, the world lookup happens on the client thread.
    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet) || packet.getStatus() != 3) return;
        mc.execute(() -> {
            if (mc.world == null) return;
            Entity e = packet.getEntity(mc.world);
            if (e instanceof LivingEntity living) tryTrigger(living);
        });
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null) return;

        long now = System.currentTimeMillis();
        long window = (long) (attackWindow.get() * 1000);

        ObjectIterator<Int2LongMap.Entry> it = attacked.int2LongEntrySet().iterator();
        List<LivingEntity> dead = null;
        while (it.hasNext()) {
            Int2LongMap.Entry entry = it.next();
            if (now - entry.getLongValue() > window) {
                it.remove();
                continue;
            }
            if (mc.world.getEntityById(entry.getIntKey()) instanceof LivingEntity living && (living.isDead() || living.getHealth() <= 0)) {
                if (dead == null) dead = new ArrayList<>();
                dead.add(living);
            }
        }
        if (dead != null) dead.forEach(this::tryTrigger);

        tickBolts();
    }

    /** Fires once: the id is removed from the map, so a second signal for the same death does nothing. */
    private void tryTrigger(LivingEntity victim) {
        if (!attacked.containsKey(victim.getId())) return;
        long hitTime = attacked.remove(victim.getId());
        if (System.currentTimeMillis() - hitTime > (long) (attackWindow.get() * 1000)) return;

        if (lightning.get()) spawnLightning(victim);
        if (particles.get()) spawnBurst(victim);
        if (sound.get() && mc.world != null) {
            mc.world.playSoundClient(soundType.get().event, SoundCategory.PLAYERS, volume.get().floatValue(), pitch.get().floatValue());
        }
    }

    private boolean isValidTarget(Entity e) {
        if (playersOnly.get()) return e instanceof PlayerEntity;
        return e instanceof LivingEntity;
    }

    // Lightning

    private void spawnLightning(LivingEntity victim) {
        LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
        bolt.setId(nextBoltId--);
        bolt.setCosmetic(true);
        bolt.setPosition(victim.getX(), victim.getY(), victim.getZ());
        mc.world.addEntity(bolt);
        bolts.add(bolt);
        boltAges.add(0);
    }

    /** Vanilla client-side LightningEntity#tick discards itself after its flashes; this is a safety net. */
    private void tickBolts() {
        for (int i = bolts.size() - 1; i >= 0; i--) {
            LightningEntity bolt = bolts.get(i);
            int age = boltAges.get(i) + 1;
            if (bolt.isRemoved()) {
                bolts.remove(i);
                boltAges.remove(i);
            } else if (age > 60) {
                mc.world.removeEntity(bolt.getId(), Entity.RemovalReason.DISCARDED);
                bolts.remove(i);
                boltAges.remove(i);
            } else {
                boltAges.set(i, age);
            }
        }
    }

    private void clearBolts() {
        if (mc.world != null) {
            for (LightningEntity bolt : bolts) {
                if (!bolt.isRemoved()) mc.world.removeEntity(bolt.getId(), Entity.RemovalReason.DISCARDED);
            }
        }
        bolts.clear();
        boltAges.clear();
    }

    // Particles

    private void spawnBurst(LivingEntity victim) {
        DustParticleEffect dust = new DustParticleEffect(particleColor.get().getPacked() & 0xFFFFFF, particleSize.get().floatValue());
        int perRing = particleCount.get();
        int ringCount = rings.get();
        double r = radius.get();
        double v = burstSpeed.get();
        double height = victim.getHeight();

        for (int ring = 0; ring < ringCount; ring++) {
            double y = victim.getY() + (ringCount == 1 ? height / 2 : height * ring / (ringCount - 1));
            double offset = ring * Math.PI / perRing; // stagger rings
            for (int i = 0; i < perRing; i++) {
                double angle = offset + i * (Math.PI * 2 / perRing);
                double cos = Math.cos(angle), sin = Math.sin(angle);
                mc.world.addParticleClient(dust,
                    victim.getX() + cos * r, y, victim.getZ() + sin * r,
                    cos * v, 0.05, sin * v);
            }
        }
    }

    public enum KillSound {
        Thunder("Thunder", SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER),
        LevelUp("Level Up", SoundEvents.ENTITY_PLAYER_LEVELUP),
        Orb("XP Orb", SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP),
        Anvil("Anvil", SoundEvents.BLOCK_ANVIL_LAND),
        Bell("Bell", SoundEvents.BLOCK_BELL_USE),
        Totem("Totem", SoundEvents.ITEM_TOTEM_USE),
        Wither("Wither Spawn", SoundEvents.ENTITY_WITHER_SPAWN),
        Dragon("Dragon Growl", SoundEvents.ENTITY_ENDER_DRAGON_GROWL),
        Blaze("Blaze Death", SoundEvents.ENTITY_BLAZE_DEATH);

        private final String title;
        final SoundEvent event;

        KillSound(String title, SoundEvent event) {
            this.title = title;
            this.event = event;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}
