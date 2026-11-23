/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.mixin.vetmo;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.visuals.BlockOutline;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class VetmoBlockOutlineWorldRendererMixin {
    // private void drawBlockOutline(MatrixStack, VertexConsumer, Entity, double, double, double, BlockPos, BlockState, int)
    // verified with javap (1.21.7 yarn build.1); called from renderTargetBlockOutline (twice with high-contrast outline)
    @Inject(method = "drawBlockOutline", at = @At("HEAD"), cancellable = true)
    private void vetmo$onDrawBlockOutline(MatrixStack matrices, VertexConsumer vertexConsumer, Entity entity, double cameraX, double cameraY, double cameraZ, BlockPos pos, BlockState state, int color, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;

        BlockOutline module = modules.get(BlockOutline.class);
        if (module != null && module.replaceVanilla(pos, state)) ci.cancel();
    }
}
