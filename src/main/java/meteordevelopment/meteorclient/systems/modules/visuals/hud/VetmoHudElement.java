/* This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0). */

package meteordevelopment.meteorclient.systems.modules.visuals.hud;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** HUD element that renders a {@link VetmoHudModule}. Position lives in the HUD, settings live in the module. */
public class VetmoHudElement extends HudElement {
    private static final Color DISABLED_BG = new Color(30, 5, 8, 120);
    private static final Color DISABLED_TEXT = new Color(255, 120, 120);

    private final Module module;
    private final VetmoHudModule hudModule;

    public VetmoHudElement(HudElementInfo<?> info, Module module) {
        super(info);
        this.module = module;
        this.hudModule = (VetmoHudModule) module;
    }

    /** Visibility is controlled by the module (ClickGUI / bind), not by the HUD element toggle. */
    @Override
    public boolean isActive() {
        return true;
    }

    public Module getModule() {
        return module;
    }

    public VetmoHudModule getHudModule() {
        return hudModule;
    }

    /** Called from the HUD editor on mouse wheel. */
    public void scaleBy(double delta) {
        Setting<Double> setting = hudModule.hudScale();
        double value = setting.get() + delta;
        if (setting instanceof DoubleSetting ds) value = Math.max(ds.min, Math.min(ds.max, value));
        value = Math.round(value * 100.0) / 100.0;
        setting.set(value);
    }

    @Override
    public void render(HudRenderer renderer) {
        try {
            renderSafe(renderer);
        } catch (Throwable t) {
            // Never crash the game because of a HUD module: log, switch the module off and tell the user.
            meteordevelopment.meteorclient.MeteorClient.LOG.error("Vetmo HUD module '{}' failed to render, disabling it", module.name, t);
            if (module.isActive()) module.toggle();
            meteordevelopment.meteorclient.utils.player.ChatUtils.error("%s failed to render and was disabled: %s", module.title, String.valueOf(t));
        }
    }

    private void renderSafe(HudRenderer renderer) {
        boolean editor = isInEditor();
        double guiScale = mc.getWindow().getScaleFactor();

        if (!module.isActive()) {
            if (editor) {
                String text = module.title + " (off)";
                double w = renderer.textWidth(text, true) + 6, h = renderer.textHeight(true) + 4;
                setSize(w, h);
                renderer.quad(x, y, w, h, DISABLED_BG);
                renderer.text(text, x + 3, y + 2, DISABLED_TEXT, true);
            } else {
                setSize(1, 1);
            }
            return;
        }

        double scale = hudModule.hudScale().get();
        double width = Math.max(1, hudModule.getHudWidth() * scale * guiScale);
        double height = Math.max(1, hudModule.getHudHeight() * scale * guiScale);
        setSize(width, height);

        if (!editor && !hudModule.shouldRenderHud()) return;

        DrawContext context = renderer.drawContext;
        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        // HUD boxes are in real pixels, DrawContext works in GUI units.
        matrices.scale((float) (1.0 / guiScale));
        matrices.translate(x, y);
        matrices.scale((float) (guiScale * scale));
        try {
            hudModule.renderHud(context, tickDelta, editor);
        } finally {
            matrices.popMatrix();
        }
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        list.add(theme.label("Settings of this element are in the ClickGUI module '" + module.title + "'."));
        list.add(theme.label("Mouse wheel over the element in the HUD editor changes its size."));
        WButton open = list.add(theme.button("Open module settings")).widget();
        open.action = () -> mc.setScreen(GuiThemes.get().moduleScreen(module));
        return list;
    }
}
