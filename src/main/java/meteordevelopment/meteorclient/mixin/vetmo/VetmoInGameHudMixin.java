/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.mixin.vetmo;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.visuals.CustomCrosshair;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class VetmoInGameHudMixin {
    // private void renderCrosshair(DrawContext, RenderTickCounter) - verified with javap (1.21.7 yarn build.1)
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void vetmo$onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;

        CustomCrosshair crosshair = modules.get(CustomCrosshair.class);
        if (crosshair == null || !crosshair.isActive() || !crosshair.shouldReplace()) return;

        crosshair.render(context, tickCounter.getTickProgress(true));
        ci.cancel();
    }
}
