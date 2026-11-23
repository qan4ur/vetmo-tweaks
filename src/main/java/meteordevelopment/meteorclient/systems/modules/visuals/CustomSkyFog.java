/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.joml.Vector4f;

/**
 * Client-side sky color, fog color/distance and time of day.
 * Hooks: VetmoSkyClientWorldMixin (sky color + rendered time of day), VetmoSkyFogRendererMixin (fog).
 * The real time of day is untouched: no packets are cancelled, only what the client renders changes.
 * Fog is never touched while the camera entity has Blindness or Darkness.
 */
public class CustomSkyFog extends Module {
    private final SettingGroup sgSky = settings.createGroup("Sky");
    private final SettingGroup sgFog = settings.createGroup("Fog");
    private final SettingGroup sgTime = settings.createGroup("Time");

    // Sky

    private final Setting<Boolean> customSky = sgSky.add(new BoolSetting.Builder()
        .name("custom-sky-color")
        .description("Replace the sky color.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> skyColor = sgSky.add(new ColorSetting.Builder()
        .name("sky-color")
        .description("Color of the sky.")
        .defaultValue(new SettingColor(130, 0, 15))
        .visible(customSky::get)
        .build()
    );

    // Fog

    private final Setting<Boolean> customFogColor = sgFog.add(new BoolSetting.Builder()
        .name("custom-fog-color")
        .description("Replace the fog / horizon color while in air.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> fogColor = sgFog.add(new ColorSetting.Builder()
        .name("fog-color")
        .description("Color of the fog.")
        .defaultValue(new SettingColor(220, 20, 40))
        .visible(customFogColor::get)
        .build()
    );

    private final Setting<FogMode> fogMode = sgFog.add(new EnumSetting.Builder<FogMode>()
        .name("fog-distance")
        .description("Vanilla, scale the fog distance, or remove distance fog in air.")
        .defaultValue(FogMode.Vanilla)
        .build()
    );

    private final Setting<Double> fogMultiplier = sgFog.add(new DoubleSetting.Builder()
        .name("distance-multiplier")
        .description("Multiplies where the fog starts and ends. Above 1 pushes it away.")
        .defaultValue(1.5)
        .range(0.1, 5)
        .sliderRange(0.1, 3)
        .decimalPlaces(2)
        .visible(() -> fogMode.get() == FogMode.Multiplier)
        .build()
    );

    private final Setting<Boolean> affectFluids = sgFog.add(new BoolSetting.Builder()
        .name("affect-fluids")
        .description("Also apply the fog distance change under water, lava and in powder snow.")
        .defaultValue(false)
        .visible(() -> fogMode.get() != FogMode.Vanilla)
        .build()
    );

    // Time

    private final Setting<Boolean> fixedTime = sgTime.add(new BoolSetting.Builder()
        .name("fixed-time")
        .description("Render the world at a fixed time of day (client-side only).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> time = sgTime.add(new IntSetting.Builder()
        .name("time")
        .description("Time of day in ticks. 1000 = morning, 6000 = noon, 13000 = sunset, 18000 = midnight.")
        .defaultValue(18000)
        .range(0, 24000)
        .sliderRange(0, 24000)
        .visible(fixedTime::get)
        .build()
    );

    public CustomSkyFog() {
        super(Categories.Visuals, "custom-sky-fog", "Custom sky color, fog color and distance, and client-side time of day.");
    }

    // Called from mixins

    /** Packed ARGB sky color, or null for vanilla. */
    public Integer getSkyColor() {
        if (!isActive() || !customSky.get()) return null;
        return skyColor.get().getPacked();
    }

    /** Fixed time of day, or -1 for vanilla. */
    public long getFixedTime() {
        if (!isActive() || !fixedTime.get()) return -1;
        return time.get();
    }

    public Vector4f modifyFogColor(Vector4f original, CameraSubmersionType submersion) {
        if (!isActive() || !customFogColor.get() || isFluid(submersion) || hasVisionEffect()) return original;
        return fogColor.get().getVec4f();
    }

    public boolean shouldModifyFogDistance(CameraSubmersionType submersion) {
        if (!isActive() || fogMode.get() == FogMode.Vanilla) return false;
        if (hasVisionEffect()) return false;
        return affectFluids.get() || !isFluid(submersion);
    }

    public boolean disableFog() {
        return fogMode.get() == FogMode.Disabled;
    }

    public float fogMultiplier() {
        return fogMultiplier.get().floatValue();
    }

    private static boolean isFluid(CameraSubmersionType type) {
        return type == CameraSubmersionType.WATER || type == CameraSubmersionType.LAVA || type == CameraSubmersionType.POWDER_SNOW;
    }

    private boolean hasVisionEffect() {
        Entity camera = mc.getCameraEntity();
        return camera instanceof LivingEntity living && (living.hasStatusEffect(StatusEffects.BLINDNESS) || living.hasStatusEffect(StatusEffects.DARKNESS));
    }

    public enum FogMode {
        Vanilla,
        Multiplier,
        Disabled
    }
}
