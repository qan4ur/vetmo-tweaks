/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.mixin.vetmo;

import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImageBackedTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OverlayTexture.class)
public interface VetmoOverlayTextureAccessor {
    // private final NativeImageBackedTexture texture - verified with javap (1.21.7 yarn build.1)
    @Accessor("texture")
    NativeImageBackedTexture vetmo$getTexture();
}
