/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.mixin.vetmo;

import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.visuals.GlintColor;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * public AbstractTexture getTexture(Identifier) - verified with javap (1.21.7 yarn build.1).
 * RenderPhase.Texture resolves its texture through this method every time a layer is set up,
 * so redirecting the two glint identifiers recolors every glint render layer.
 */
@Mixin(TextureManager.class)
public abstract class VetmoGlintTextureManagerMixin {
    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true)
    private void vetmo$onGetTexture(Identifier id, CallbackInfoReturnable<AbstractTexture> cir) {
        if (GlintColor.bypass) return;

        Modules modules = Modules.get();
        if (modules == null) return;

        GlintColor module = modules.get(GlintColor.class);
        if (module == null) return;

        AbstractTexture override = module.getOverride(id);
        if (override != null) cir.setReturnValue(override);
    }
}
