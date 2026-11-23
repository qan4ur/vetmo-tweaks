/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.mixin.vetmo;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.visuals.PotionHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the vanilla top-right status effect icons while PotionHud is active with "hide-vanilla-icons" on. */
@Mixin(InGameHud.class)
public abstract class VetmoPotionHudMixin {
    @Inject(method = "renderStatusEffectOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
    private void vetmo$hideVanillaEffects(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        Modules modules = Modules.get();
        if (modules == null) return;
        PotionHud hud = modules.get(PotionHud.class);
        if (hud != null && hud.shouldHideVanilla()) ci.cancel();
    }
}
