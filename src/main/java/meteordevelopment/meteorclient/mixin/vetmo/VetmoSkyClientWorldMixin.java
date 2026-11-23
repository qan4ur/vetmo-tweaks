/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.mixin.vetmo;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.visuals.CustomSkyFog;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Verified with javap (1.21.7 yarn build.1):
 * - ClientWorld: public int getSkyColor(Vec3d, float)
 * - WorldAccess: public default long getLunarTime() (= getLevelProperties().getTimeOfDay()); ClientWorld does not override it.
 *   LunarWorldView.getSkyAngle(float) / getMoonPhase() use getLunarTime(), so overriding it here changes only the
 *   rendered sun/moon/sky position. ClientWorld$Properties.timeOfDay is left alone (tickTime() reads/writes it),
 *   so the real time keeps ticking and is correct the moment the module is turned off.
 * The mixin implements WorldAccess so the added getLunarTime() is recognised as an override and gets remapped.
 */
@Mixin(ClientWorld.class)
public abstract class VetmoSkyClientWorldMixin implements WorldAccess {
    @Inject(method = "getSkyColor", at = @At("HEAD"), cancellable = true)
    private void vetmo$onGetSkyColor(Vec3d cameraPos, float tickProgress, CallbackInfoReturnable<Integer> cir) {
        CustomSkyFog module = vetmo$module();
        if (module == null) return;

        Integer color = module.getSkyColor();
        if (color != null) cir.setReturnValue(color);
    }

    @Override
    public long getLunarTime() {
        CustomSkyFog module = vetmo$module();
        if (module != null) {
            long fixed = module.getFixedTime();
            if (fixed >= 0) return fixed;
        }

        return ((ClientWorld) (Object) this).getLevelProperties().getTimeOfDay();
    }

    @Unique
    private static CustomSkyFog vetmo$module() {
        Modules modules = Modules.get();
        if (modules == null) return null;
        return modules.get(CustomSkyFog.class);
    }
}
