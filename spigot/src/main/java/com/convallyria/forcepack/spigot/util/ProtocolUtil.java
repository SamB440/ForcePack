package com.convallyria.forcepack.spigot.util;

import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.netty.buffer.ByteBufHelper;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
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

    private static boolean warnedBadPlugin;

    /**
     * Sends a packet to a player bypassing viaversion.
     */
    public static void sendPacketBypassingVia(Player player, PacketWrapper<?> packet) {
        final User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        // This can only ever happen if there is a bad plugin creating NPCs AND adding them to the online player map
        if (user == null) {
            if (!warnedBadPlugin) {
                ForcePackSpigot.getInstance().getLogger().severe("You have a poorly coded plugin creating entirely fake players that are not specified as such/");
                ForcePackSpigot.getInstance().getLogger().severe("Additionally, this message can only appear when the player exists in the Bukkit#getOnlinePlayers map.");
                ForcePackSpigot.getInstance().getLogger().severe("This has a performance impact, including memory leaks, and must be fixed.");
                warnedBadPlugin = true;
            }
            return;
        }

        final Object channel = user.getChannel();
        if (!ChannelHelper.isOpen(channel)) return;

        sendBypassingPacket(user, channel, packet);
    }

    private static void sendBypassingPacket(User user, Object channel, PacketWrapper<?> packet) {
        // If ViaVersion is present in the pipeline
        if (user.getClientVersion().isNewerThan(PacketEvents.getAPI().getServerManager().getVersion().toClientVersion())
                && ChannelHelper.getPipelineHandler(channel, "via-encoder") != null) {
            // Allocate the buffer for the wrapper
            packet.buffer = ChannelHelper.pooledByteBuf(channel);

            // We need to convert the packet ID to the appropriate one for that user's version
            int id = packet.getPacketTypeData().getPacketType().getId(user.getClientVersion());
            // Write the packet ID to the buffer
            ByteBufHelper.writeVarInt(packet.buffer, id);
            // Change the version in which the wrapper is processed
            packet.setServerVersion(user.getClientVersion().toServerVersion());
            // Write the packet content
            packet.write();
            // Send the buffer
            ChannelHelper.writeAndFlushInContext(channel, "via-encoder", packet.buffer);
        } else {
            user.sendPacketSilently(packet);
        }
    }
}
