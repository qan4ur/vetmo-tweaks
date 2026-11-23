/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.mixin.vetmo;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.visuals.WorldBorder;
import net.minecraft.client.render.WorldBorderRendering;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Verified with javap (1.21.7 yarn build.1), WorldBorderRendering:
 * public void render(net.minecraft.world.border.WorldBorder, Vec3d, double viewDistanceBlocks, double farPlaneDistance)
 * - color: single call to WorldBorderStage.getColor()I
 * - scrolling animation: single call to Util.getMeasuringTimeMs()J (texture offset = time % 3000 / 3000)
 * - alpha: single call to MathHelper.clamp(DDD)D (distance based fade, passed as the color's alpha)
 */
@Mixin(WorldBorderRendering.class)
public abstract class VetmoWorldBorderRenderingMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void vetmo$onRender(net.minecraft.world.border.WorldBorder border, Vec3d cameraPos, double viewDistanceBlocks, double farPlaneDistance, CallbackInfo ci) {
        WorldBorder module = vetmo$module();
        if (module != null && module.hide()) ci.cancel();
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorderStage;getColor()I"))
    private int vetmo$modifyColor(int original) {
        WorldBorder module = vetmo$module();
        return module != null ? module.getColor(original) : original;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Util;getMeasuringTimeMs()J"))
    private long vetmo$freezeAnimation(long original) {
        WorldBorder module = vetmo$module();
        return module != null && module.isStatic() ? 0L : original;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;clamp(DDD)D"))
    private double vetmo$modifyAlpha(double original) {
        WorldBorder module = vetmo$module();
        return module != null ? module.getAlpha(original) : original;
    }

    @Unique
    private static WorldBorder vetmo$module() {
        Modules modules = Modules.get();
        if (modules == null) return null;
        return modules.get(WorldBorder.class);
    }
}
