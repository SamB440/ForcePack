package net.islandearth.forcepack.spigot.utils;

import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

public class GeyserUtil {

    public static boolean isBedrockPlayer(final Player player) {
        return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
    }
}
