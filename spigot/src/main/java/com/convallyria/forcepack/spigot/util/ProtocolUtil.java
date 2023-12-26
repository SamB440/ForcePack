package com.convallyria.forcepack.spigot.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.viaversion.viaversion.api.Via;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ProtocolUtil {

    public static int getProtocolVersion(Player player) {
        final boolean viaversion = Bukkit.getPluginManager().isPluginEnabled("ViaVersion");
        return viaversion
                ? Via.getAPI().getPlayerVersion(player)
                : PacketEvents.getAPI().getPlayerManager().getUser(player).getClientVersion().getProtocolVersion();
    }
}
