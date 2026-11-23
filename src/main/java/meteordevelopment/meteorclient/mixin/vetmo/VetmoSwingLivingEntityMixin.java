/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.mixin.vetmo;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.visuals.SwingAnimations;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class VetmoSwingLivingEntityMixin {
    // private int getHandSwingDuration() - verified with javap (1.21.7 yarn build.1), 3 return points, all modified
    @ModifyReturnValue(method = "getHandSwingDuration", at = @At("RETURN"))
    private int vetmo$modifySwingDuration(int original) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || (Object) this != mc.player) return original;

        Modules modules = Modules.get();
        if (modules == null) return original;

        SwingAnimations module = modules.get(SwingAnimations.class);
        return module != null ? module.modifySwingDuration(original) : original;
    }
}
