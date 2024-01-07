package com.convallyria.forcepack.spigot.util;

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

    /**
     * Sends a packet to a player bypassing viaversion.
     */
    public static void sendPacketBypassingVia(Player player, PacketWrapper<?> packet) {
        final User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        final Object channel = user.getChannel();
        if (!ChannelHelper.isOpen(channel)) return;

        // If ViaVersion is present in the pipeline
        if (user.getClientVersion().toServerVersion() != PacketEvents.getAPI().getServerManager().getVersion()
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
