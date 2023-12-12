package com.convallyria.forcepack.spigot.util;

import com.viaversion.viaversion.api.Via;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ViaVersionUtil {

    public static int getProtocolVersion(Player player) {
        final boolean viaversion = Bukkit.getPluginManager().isPluginEnabled("ViaVersion");
        return viaversion ? Via.getAPI().getPlayerVersion(player) : 765; // 765 is 1.20.3 - default to this
    }
}
