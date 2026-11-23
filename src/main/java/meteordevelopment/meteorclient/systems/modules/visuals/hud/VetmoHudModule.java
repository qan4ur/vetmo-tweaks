/* This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0). */

package meteordevelopment.meteorclient.systems.modules.visuals.hud;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.hud.XAnchor;
import meteordevelopment.meteorclient.systems.hud.YAnchor;
import net.minecraft.client.gui.DrawContext;

/**
 * A Vetmo module that draws something on screen. The module keeps all its settings in the ClickGUI,
 * while the drawing goes through a {@link VetmoHudElement} that can be moved with the mouse and resized
 * (mouse wheel) in the HUD editor.
 *
 * <p>All sizes and coordinates passed to/returned from these methods are in <b>GUI units at scale 1</b>.
 * The element translates the DrawContext so that (0, 0) is the top-left corner of the element box and
 * applies the GUI scale and {@link #hudScale()}.</p>
 *
 * <p>Implementations must extend {@link meteordevelopment.meteorclient.systems.modules.Module}.</p>
 */
public interface VetmoHudModule {
    /** Width of the element in GUI units at scale 1 (may change every frame). */
    double getHudWidth();

    /** Height of the element in GUI units at scale 1 (may change every frame). */
    double getHudHeight();

    /**
     * Draws the element with its top-left corner at (0, 0).
     *
     * @param inEditor true while the HUD editor (or main menu) is open — draw a preview if there is nothing to show
     */
    void renderHud(DrawContext context, float tickDelta, boolean inEditor);

    /** Whether anything should be drawn right now. The editor still calls {@link #renderHud} with inEditor = true. */
    default boolean shouldRenderHud() {
        return true;
    }

    /** Scale setting of the element (in the module's ClickGUI settings, changed by mouse wheel in the HUD editor). */
    Setting<Double> hudScale();

    /** Default position of the element box (in real pixels, relative to the anchors) when it is first added. */
    default int defaultHudX() { return 4; }

    default int defaultHudY() { return 4; }

    default XAnchor defaultHudXAnchor() { return XAnchor.Left; }

    default YAnchor defaultHudYAnchor() { return YAnchor.Top; }
}
