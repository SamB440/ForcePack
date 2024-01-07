package com.convallyria.forcepack.spigot.listener;

import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.event.MultiVersionResourcePackStatusEvent;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.ConnectionState;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.PacketEventsImplHelper;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientResourcePackStatus;
import io.github.retrooper.packetevents.injector.handlers.PacketEventsDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.kyori.adventure.resource.ResourcePackStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class PacketListener extends PacketListenerAbstract {

    private final ForcePackSpigot plugin;

    public PacketListener(ForcePackSpigot plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.RESOURCE_PACK_STATUS) {
            WrapperPlayClientResourcePackStatus status = new WrapperPlayClientResourcePackStatus(event);
            if (event.getPlayer() == null) {
                plugin.getLogger().warning("Unable to get player for resource pack status!?!? " + event.getUser() + ", " + event.getPlayer());
                return;
            }

            final Player player = (Player) event.getPlayer();
            final WrapperPlayClientResourcePackStatus.Result result = status.getResult();
            final UUID packId = status.getPackId();
            final MultiVersionResourcePackStatusEvent packEvent = new MultiVersionResourcePackStatusEvent(player, packId, ResourcePackStatus.valueOf(result.name()), false, false);
            Bukkit.getPluginManager().callEvent(packEvent);
            event.setCancelled(packEvent.isCancelled());
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.JOIN_GAME) {
            final User user = event.getUser();
            ChannelHelper.runInEventLoop(user.getChannel(), () -> moveBeforeVia(user));
        }
    }

    /**
     * This is an extremely cursed way to move the packetevents decoder to be before viaversion.
     * We need this because we require the UUID of a resource pack response on 1.20.3+ clients.
     */
    private void moveBeforeVia(User user) {
        // We only need to do this for the client versions that send a UUID response
        if (!user.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_20_3)) return;

        final Channel channel = (Channel) user.getChannel();
        final ChannelPipeline pipeline = channel.pipeline();
        // If viaversion is present
        if (pipeline.get("via-decoder") == null) return;

        // Remove the current decoder set by packetevents
        pipeline.remove("pe-decoder-forcepack");

        // And add a replacement before viaversion
        pipeline.addBefore("via-decoder", "pe-decoder-forcepack", new PacketEventsDecoder(user) {
            // Override to change autoProtocolTranslation -> false, this makes the event always use the client version.
            @Override
            public void read(ChannelHandlerContext ctx, ByteBuf input, List<Object> out) throws Exception {
                PacketEventsImplHelper.handleServerBoundPacket(ctx.channel(), user, player, input, user.getDecoderState() != ConnectionState.PLAY);
                out.add(input.retain());
            }
        });
    }
}
