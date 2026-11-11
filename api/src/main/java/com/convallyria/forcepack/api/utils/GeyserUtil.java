package com.convallyria.forcepack.api.utils;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.api.GeyserApi;

import java.util.UUID;

public class GeyserUtil {

    public static boolean isGeyserInstalledHere = false;

    public static boolean isBedrockPlayer(final UUID player) {
        if (isGeyserInstalledHere) {
            try {
                return GeyserApi.api().isBedrockPlayer(player);
            } catch (Throwable ignored) {
                // Geyser still starting, reloading or failed: fall back to Floodgate
            }
        }

        try {
            final FloodgateApi floodgate = FloodgateApi.getInstance();
            if (floodgate != null) {
                return floodgate.isFloodgatePlayer(player) || floodgate.isFloodgateId(player);
            }
        } catch (Throwable ignored) {
            // Neither Geyser nor Floodgate is available (NoClassDefFoundError): treat as Java player
        }
        return false;
    }
}
