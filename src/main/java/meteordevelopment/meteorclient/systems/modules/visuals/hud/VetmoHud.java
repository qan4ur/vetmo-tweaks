/* This file is part of Vetmo Tweaks, a fork of Meteor Client (GPL-3.0). */

package meteordevelopment.meteorclient.systems.modules.visuals.hud;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.orbit.EventHandler;

import java.util.*;

/**
 * Glue between Vetmo HUD modules and Meteor's HUD: registers one HUD element type per module and makes sure
 * an element exists whenever the module is turned on.
 */
public final class VetmoHud {
    private static final Map<Module, HudElementInfo<VetmoHudElement>> INFOS = new LinkedHashMap<>();
    private static final Set<Module> wasActive = Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean subscribed;
    private static boolean checkAll = true;

    private VetmoHud() {}

    /** Called from {@link Hud#init()}; modules are already created at that point. */
    public static void register(Hud hud) {
        for (Module module : Modules.get().getAll()) {
            if (!(module instanceof VetmoHudModule) || INFOS.containsKey(module)) continue;

            @SuppressWarnings("unchecked")
            HudElementInfo<VetmoHudElement>[] holder = new HudElementInfo[1];
            holder[0] = new HudElementInfo<>(Hud.GROUP, module.name, module.title, module.description, () -> new VetmoHudElement(holder[0], module));

            INFOS.put(module, holder[0]);
            hud.register(holder[0]);
        }

        if (!subscribed) {
            MeteorClient.EVENT_BUS.subscribe(VetmoHud.class);
            subscribed = true;
        }
    }

    public static HudElementInfo<VetmoHudElement> getInfo(Module module) {
        return INFOS.get(module);
    }

    /** Adds the element of this module to the HUD if there is none yet. */
    public static void ensureElement(Module module) {
        HudElementInfo<VetmoHudElement> info = INFOS.get(module);
        if (info == null) return;

        Hud hud = Hud.get();
        for (HudElement element : hud) {
            if (element.info == info) return;
        }

        VetmoHudModule m = (VetmoHudModule) module;
        hud.add(info, m.defaultHudX(), m.defaultHudY(), m.defaultHudXAnchor(), m.defaultHudYAnchor());
    }

    @EventHandler
    private static void onTick(TickEvent.Post event) {
        if (Utils.isLoading()) return;

        for (Module module : INFOS.keySet()) {
            boolean active = module.isActive();
            boolean before = wasActive.contains(module);

            // Add the element when the module is switched on (or once after startup for modules that are already on).
            if (active && (!before || checkAll)) ensureElement(module);

            if (active) wasActive.add(module);
            else wasActive.remove(module);
        }

        checkAll = false;
    }
}
