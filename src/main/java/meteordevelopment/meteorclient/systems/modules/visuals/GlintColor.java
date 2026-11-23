/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.util.Optional;

/**
 * Recolors the enchantment glint. Builds tinted copies of the vanilla glint textures (luminance x color) and
 * VetmoGlintTextureManagerMixin hands them out from TextureManager.getTexture() for the two glint identifiers.
 * The vanilla textures are never replaced or closed, so disabling just stops the redirect. A resource reload
 * recreates the vanilla GpuTexture, which we detect each tick and rebuild from the new resource.
 */
public class GlintColor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General

    private final Setting<Boolean> items = sgGeneral.add(new BoolSetting.Builder()
        .name("items")
        .description("Recolor the glint on items (held, dropped, in GUIs).")
        .defaultValue(true)
        .onChanged(v -> markDirty())
        .build()
    );

    private final Setting<Boolean> armor = sgGeneral.add(new BoolSetting.Builder()
        .name("armor-and-entities")
        .description("Recolor the glint on worn armor, elytra, tridents and shields.")
        .defaultValue(true)
        .onChanged(v -> markDirty())
        .build()
    );

    private final Setting<Double> brightness = sgGeneral.add(new DoubleSetting.Builder()
        .name("brightness")
        .description("Intensity of the tinted glint.")
        .defaultValue(1.2)
        .range(0.1, 3)
        .sliderRange(0.1, 2.5)
        .decimalPlaces(2)
        .onChanged(v -> markDirty())
        .build()
    );

    // Colors

    private final Setting<SettingColor> itemColor = sgColors.add(new ColorSetting.Builder()
        .name("item-color")
        .description("Glint color for items.")
        .defaultValue(new SettingColor(220, 20, 40))
        .visible(items::get)
        .onChanged(v -> markDirty())
        .build()
    );

    private final Setting<Boolean> sameColor = sgColors.add(new BoolSetting.Builder()
        .name("armor-uses-item-color")
        .description("Use the item color for armor too.")
        .defaultValue(true)
        .visible(armor::get)
        .onChanged(v -> markDirty())
        .build()
    );

    private final Setting<SettingColor> armorColor = sgColors.add(new ColorSetting.Builder()
        .name("armor-color")
        .description("Glint color for armor and entities.")
        .defaultValue(new SettingColor(130, 0, 15))
        .visible(() -> armor.get() && !sameColor.get())
        .onChanged(v -> markDirty())
        .build()
    );

    /** Set while we read the vanilla texture so the mixin doesn't redirect our own lookup. */
    public static boolean bypass;

    private final Tinted item = new Tinted(ItemRenderer.ITEM_ENCHANTMENT_GLINT, "vetmo_glint_item");
    private final Tinted entity = new Tinted(ItemRenderer.ENTITY_ENCHANTMENT_GLINT, "vetmo_glint_entity");
    private boolean dirty = true;

    // Last applied colors, rainbow colors change without onChanged
    private int lastItemPacked, lastArmorPacked;

    public GlintColor() {
        super(Categories.Visuals, "glint-color", "Recolors the enchantment glint on items and armor.");
    }

    @Override
    public void onActivate() {
        // Built on the next tick (not here: the module can be enabled from config before the render system is up)
        dirty = true;
    }

    @Override
    public void onDeactivate() {
        item.close();
        entity.close();
    }

    private void markDirty() {
        dirty = true;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null) return;
        update();
    }

    private void update() {
        SettingColor ic = itemColor.get();
        SettingColor ac = sameColor.get() ? ic : armorColor.get();
        if (ic.getPacked() != lastItemPacked || ac.getPacked() != lastArmorPacked) dirty = true;

        boolean rebuild = dirty;
        dirty = false;

        if (items.get()) item.update(ic, rebuild);
        else item.close();

        if (armor.get()) entity.update(ac, rebuild);
        else entity.close();

        lastItemPacked = ic.getPacked();
        lastArmorPacked = ac.getPacked();
    }

    /** Called from VetmoGlintTextureManagerMixin. Returns null to use vanilla. */
    public AbstractTexture getOverride(Identifier id) {
        if (bypass || !isActive()) return null;
        if (id.equals(item.id)) return item.texture;
        if (id.equals(entity.id)) return entity.texture;
        return null;
    }

    private class Tinted {
        final Identifier id;
        final String name;
        NativeImageBackedTexture texture;
        GpuTexture source;
        boolean failed;

        Tinted(Identifier id, String name) {
            this.id = id;
            this.name = name;
        }

        void update(SettingColor color, boolean rebuild) {
            GpuTexture current = vanillaGpuTexture();
            if (current != source) {
                // First build or the resource pack was reloaded
                rebuild = true;
                failed = false;
            }
            if (failed || (!rebuild && texture != null)) return;

            source = current;
            close();

            try {
                Optional<Resource> resource = mc.getResourceManager().getResource(id);
                if (resource.isEmpty()) {
                    failed = true;
                    return;
                }

                NativeImage image;
                try (InputStream in = resource.get().getInputStream()) {
                    image = NativeImage.read(in);
                }

                tint(image, color);

                NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> name, image);
                tex.setClamp(false);       // glint scrolls, needs REPEAT
                tex.setFilter(true, false); // vanilla glint is blurred
                texture = tex;
            } catch (Throwable t) {
                failed = true;
                MeteorClient.LOG.error("Vetmo GlintColor: failed to build tinted glint for {}", id, t);
            }
        }

        private void tint(NativeImage image, SettingColor color) {
            double mul = brightness.get();
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    int argb = image.getColorArgb(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;

                    // Brightest channel keeps the glint's strength (the vanilla glint is purple, plain luminance would be too dark)
                    double l = Math.max(r, Math.max(g, b)) / 255.0 * mul;

                    int nr = clamp(color.r * l);
                    int ng = clamp(color.g * l);
                    int nb = clamp(color.b * l);
                    image.setColorArgb(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
                }
            }
        }

        private int clamp(double v) {
            return (int) Math.max(0, Math.min(255, Math.round(v)));
        }

        private GpuTexture vanillaGpuTexture() {
            bypass = true;
            try {
                return mc.getTextureManager().getTexture(id).getGlTexture();
            } catch (Throwable t) {
                return null;
            } finally {
                bypass = false;
            }
        }

        void close() {
            if (texture != null) {
                texture.close();
                texture = null;
            }
        }
    }
}
