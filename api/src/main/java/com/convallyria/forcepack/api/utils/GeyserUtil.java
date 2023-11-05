package com.convallyria.forcepack.api.utils;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.api.GeyserApi;

import java.util.UUID;

public class GeyserUtil {

    public static boolean isGeyserInstalledHere = false;

    public static boolean isBedrockPlayer(final UUID player) {
        if (isGeyserInstalledHere) {
            return GeyserApi.api().isBedrockPlayer(player);
        } else {
            return FloodgateApi.getInstance().isFloodgatePlayer(player) || FloodgateApi.getInstance().isFloodgateId(player);
        }
    }
}
