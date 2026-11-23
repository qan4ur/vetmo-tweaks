/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.joml.Vector3d;

/**
 * First-person hand/item tweaks. Purely client-side rendering, nothing is sent to the server.
 * Hooks: VetmoSwingHeldItemRendererMixin (transforms + equip bob), VetmoSwingLivingEntityMixin (local swing duration).
 */
public class SwingAnimations extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMainHand = settings.createGroup("Main Hand");
    private final SettingGroup sgOffHand = settings.createGroup("Off Hand");

    // General

    public final Setting<Preset> preset = sgGeneral.add(new EnumSetting.Builder<Preset>()
        .name("preset")
        .description("Classic 1.7 forces the old animation set: 6 tick swing, no swap bob and no attack-cooldown dip.")
        .defaultValue(Preset.Custom)
        .build()
    );

    private final Setting<Integer> swingDuration = sgGeneral.add(new IntSetting.Builder()
        .name("swing-duration")
        .description("Length of your first-person swing in ticks (vanilla is 6, higher is slower).")
        .defaultValue(6)
        .range(1, 30)
        .sliderRange(1, 20)
        .visible(() -> preset.get() == Preset.Custom)
        .build()
    );

    private final Setting<Boolean> ignoreEffects = sgGeneral.add(new BoolSetting.Builder()
        .name("ignore-haste-fatigue")
        .description("Use the swing duration even when Haste / Mining Fatigue would change it.")
        .defaultValue(true)
        .visible(() -> preset.get() == Preset.Custom)
        .build()
    );

    private final Setting<Boolean> noSwapAnimation = sgGeneral.add(new BoolSetting.Builder()
        .name("no-swap-animation")
        .description("Removes the lower/raise bob when switching items (1.7/1.8 style).")
        .defaultValue(true)
        .visible(() -> preset.get() == Preset.Custom)
        .build()
    );

    private final Setting<Boolean> noCooldownDip = sgGeneral.add(new BoolSetting.Builder()
        .name("no-cooldown-dip")
        .description("Removes the item dipping down after every hit because of the attack cooldown (1.8 style).")
        .defaultValue(true)
        .visible(() -> preset.get() == Preset.Custom)
        .build()
    );

    private final Setting<Boolean> affectEmptyHand = sgGeneral.add(new BoolSetting.Builder()
        .name("affect-empty-hand")
        .description("Also apply the main hand position/rotation/scale to your bare arm.")
        .defaultValue(false)
        .build()
    );

    // Main hand

    private final Setting<Double> scaleMain = sgMainHand.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Size of the item in your main hand.")
        .defaultValue(1)
        .range(0.1, 3)
        .sliderRange(0.1, 2)
        .build()
    );

    private final Setting<Vector3d> posMain = sgMainHand.add(new Vector3dSetting.Builder()
        .name("offset")
        .description("X / Y / Z offset of the main hand item.")
        .defaultValue(0, 0, 0)
        .sliderRange(-1, 1)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Vector3d> rotMain = sgMainHand.add(new Vector3dSetting.Builder()
        .name("rotation")
        .description("X / Y / Z rotation of the main hand item in degrees.")
        .defaultValue(0, 0, 0)
        .sliderRange(-180, 180)
        .decimalPlaces(0)
        .build()
    );

    // Off hand

    private final Setting<Double> scaleOff = sgOffHand.add(new DoubleSetting.Builder()
        .name("scale")
        .description("Size of the item in your off hand.")
        .defaultValue(1)
        .range(0.1, 3)
        .sliderRange(0.1, 2)
        .build()
    );

    private final Setting<Vector3d> posOff = sgOffHand.add(new Vector3dSetting.Builder()
        .name("offset")
        .description("X / Y / Z offset of the off hand item.")
        .defaultValue(0, 0, 0)
        .sliderRange(-1, 1)
        .decimalPlaces(2)
        .build()
    );

    private final Setting<Vector3d> rotOff = sgOffHand.add(new Vector3dSetting.Builder()
        .name("rotation")
        .description("X / Y / Z rotation of the off hand item in degrees.")
        .defaultValue(0, 0, 0)
        .sliderRange(-180, 180)
        .decimalPlaces(0)
        .build()
    );

    public SwingAnimations() {
        super(Categories.Visuals, "swing-animations", "Customize your first-person item position, scale and swing (1.7/1.8 style options).");
    }

    // Called from mixins

    private boolean classic() {
        return preset.get() == Preset.Classic17;
    }

    /** Only modifies the local player's swing, and only in first person. */
    public int modifySwingDuration(int original) {
        if (!isActive() || mc.options == null || !mc.options.getPerspective().isFirstPerson()) return original;

        if (classic()) return 6;
        if (!ignoreEffects.get() && original != 6) return original;
        return swingDuration.get();
    }

    public boolean noSwapAnimation() {
        return isActive() && (classic() || noSwapAnimation.get());
    }

    public boolean noCooldownDip() {
        return isActive() && (classic() || noCooldownDip.get());
    }

    public void transformItem(Hand hand, MatrixStack matrices) {
        if (!isActive()) return;

        if (hand == Hand.MAIN_HAND) apply(matrices, posMain.get(), rotMain.get(), scaleMain.get());
        else apply(matrices, posOff.get(), rotOff.get(), scaleOff.get());
    }

    public void transformEmptyHand(MatrixStack matrices) {
        if (!isActive() || !affectEmptyHand.get()) return;
        apply(matrices, posMain.get(), rotMain.get(), scaleMain.get());
    }

    private void apply(MatrixStack matrices, Vector3d pos, Vector3d rot, double scale) {
        if (pos.x != 0 || pos.y != 0 || pos.z != 0) matrices.translate((float) pos.x, (float) pos.y, (float) pos.z);
        if (rot.x != 0) matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rot.x));
        if (rot.y != 0) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rot.y));
        if (rot.z != 0) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rot.z));
        if (scale != 1) matrices.scale((float) scale, (float) scale, (float) scale);
    }

    public enum Preset {
        Custom,
        Classic17;

        @Override
        public String toString() {
            return this == Custom ? "Custom" : "Classic 1.7";
        }
    }
}
