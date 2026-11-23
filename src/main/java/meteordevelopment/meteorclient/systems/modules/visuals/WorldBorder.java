/*
 * This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0).
 */

package meteordevelopment.meteorclient.systems.modules.visuals;

import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

/**
 * World border visuals only (the border itself, its collision and damage are untouched).
 * Hook: VetmoWorldBorderRenderingMixin on net.minecraft.client.render.WorldBorderRendering.render.
 */
public class WorldBorder extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("mode")
        .description("Hide: don't render the border. Static: custom color without the scrolling animation.")
        .defaultValue(Mode.Static)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Border color (also used while the border grows or shrinks).")
        .defaultValue(new SettingColor(220, 20, 40))
        .visible(() -> mode.get() == Mode.Static)
        .build()
    );

    private final Setting<Integer> alpha = sgGeneral.add(new IntSetting.Builder()
        .name("alpha")
        .description("Maximum opacity of the border.")
        .defaultValue(160)
        .range(0, 255)
        .sliderRange(0, 255)
        .visible(() -> mode.get() == Mode.Static)
        .build()
    );

    private final Setting<Boolean> fade = sgGeneral.add(new BoolSetting.Builder()
        .name("fade-with-distance")
        .description("Fade the border in as you get closer like vanilla. Off keeps a constant opacity while it's rendered.")
        .defaultValue(true)
        .visible(() -> mode.get() == Mode.Static)
        .build()
    );

    public WorldBorder() {
        super(Categories.Visuals, "world-border", "Hide the world border or render it in a static custom color.");
    }

    // Called from the mixin

    public boolean hide() {
        return isActive() && mode.get() == Mode.Hide;
    }

    public boolean isStatic() {
        return isActive() && mode.get() == Mode.Static;
    }

    /** RGB color (alpha bits ignored by vanilla, which only reads red/green/blue from the stage color). */
    public int getColor(int original) {
        if (!isStatic()) return original;
        SettingColor c = color.get();
        return 0xFF000000 | (c.r << 16) | (c.g << 8) | c.b;
    }

    public double getAlpha(double original) {
        if (!isStatic()) return original;
        double max = alpha.get() / 255.0;
        return fade.get() ? original * max : max;
    }

    public enum Mode {
        Hide,
        Static
    }
}
