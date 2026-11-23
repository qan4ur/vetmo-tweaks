/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.vetmo.VetmoOverlayTextureAccessor;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

/**
 * Recolors the entity hurt tint. In 1.21.7 the tint is the 16x16 "Entity Color Overlay" texture owned by
 * {@link net.minecraft.client.render.OverlayTexture}: rows y < 8 are the hurt color (vanilla ARGB 0xB2FF0000),
 * rows y >= 8 are the white flash ramp, which we leave untouched.
 */
public class HitColor extends Module {
    /** Vanilla hurt color, from OverlayTexture.<init> (ldc -1291911168). */
    private static final int VANILLA_HURT_ARGB = 0xB2FF0000;

    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<SettingColor> color = sgColors.add(new ColorSetting.Builder()
        .name("hit-color")
        .description("Tint applied to entities when they take damage. Alpha controls the strength.")
        .defaultValue(new SettingColor(220, 20, 40, 178))
        .onChanged(c -> { if (isActive()) apply(); })
        .build()
    );

    private int lastApplied;
    private boolean applied;

    public HitColor() {
        super(Categories.Visuals, "hit-color", "Changes the red hurt tint of entities to a custom color.");
    }

    @Override
    public void onActivate() {
        applied = false;
        apply();
    }

    @Override
    public void onDeactivate() {
        writeHurtRows(VANILLA_HURT_ARGB);
        applied = false;
    }

    // Also catches rainbow colors and the case where the module was enabled before the game renderer existed.
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!applied || color.get().getPacked() != lastApplied) apply();
    }

    private void apply() {
        int argb = color.get().getPacked();
        if (writeHurtRows(argb)) {
            lastApplied = argb;
            applied = true;
        }
    }

    private boolean writeHurtRows(int argb) {
        if (mc.gameRenderer == null || mc.gameRenderer.getOverlayTexture() == null) return false;

        NativeImageBackedTexture texture = ((VetmoOverlayTextureAccessor) mc.gameRenderer.getOverlayTexture()).vetmo$getTexture();
        if (texture == null) return false;
        NativeImage image = texture.getImage();
        if (image == null) return false;

        // Same layout as the vanilla constructor: setColorArgb(x, y, color) for y in [0, 8), x in [0, 16)
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 16; x++) {
                image.setColorArgb(x, y, argb);
            }
        }

        texture.upload();
        return true;
    }
}
