/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.network;

public class OnlinePlayers {
    private static long lastPingTime;

    private OnlinePlayers() {
    }

    public static void update() {
        // Vetmo Tweaks: no telemetry pings to meteorclient.com
    }

    public static void leave() {
        // Vetmo Tweaks: no telemetry pings to meteorclient.com
    }
}
