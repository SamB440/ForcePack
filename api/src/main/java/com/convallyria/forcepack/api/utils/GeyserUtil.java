package com.convallyria.forcepack.api.utils;

import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class GeyserUtil {

    public static boolean isBedrockPlayer(final UUID player) {
        return FloodgateApi.getInstance().isFloodgatePlayer(player);
    }
}
