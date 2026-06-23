package com.convallyria.forcepack.paper.listener;

import com.convallyria.forcepack.paper.ForcePackPaper;
import com.convallyria.forcepack.paper.event.MultiVersionResourcePackStatusEvent;
import com.convallyria.forcepack.paper.player.ForcePackPaperPlayer;
import com.convallyria.forcepack.paper.util.GameProfile;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.event.UserDisconnectEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.configuration.client.WrapperConfigClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientResourcePackStatus;
import net.kyori.adventure.resource.ResourcePackStatus;
import org.bukkit.Bukkit;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class PacketListener extends PacketListenerAbstract {

    private static final String STATUS_CHANNEL = "forcepack:status";

    private final ForcePackPaper plugin;

    public PacketListener(ForcePackPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        final PacketTypeCommon type = event.getPacketType();
        if (isResourcePackStatus(type)) {
            handleResourcePackStatus(event);
        } else if (isPluginMessage(type)) {
            handlePluginMessage(event);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS
                && PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_20_2)) {
            // We blindly add here since we can't check permissions this early on Paper
            plugin.addToWaiting(event.getUser().getUUID(), Set.of());
        }
    }

    // When a player disconnects, remove them from the waiting list
    // This is the only way to accurately detect disconnecting on both legacy and modern Paper
    @Override
    public void onUserDisconnect(UserDisconnectEvent event) {
        final User user = event.getUser();
        plugin.removeFromWaiting(user.getUUID());
    }

    private void handleResourcePackStatus(PacketReceiveEvent event) {
        final User user = event.getUser();
        final ForcePackPaperPlayer player = plugin.getForcePackPlayer(user.getUUID()).orElse(null);
        if (player == null) {
            // Player isn't valid - wasn't added to waiting (exempt or not yet tracked)
            return;
        }

        plugin.log("Received packet resource pack status from " + user.getName() + " (version: " + event.getServerVersion().getReleaseName() + ")");

        final WrapperPlayClientResourcePackStatus status = new WrapperPlayClientResourcePackStatus(event);
        final WrapperPlayClientResourcePackStatus.Result result = status.getResult();
        final UUID packId = status.getPackId();
        final GameProfile gameProfile = new GameProfile(user.getUUID(), user.getName());
        final MultiVersionResourcePackStatusEvent packEvent = new MultiVersionResourcePackStatusEvent(gameProfile, packId, ResourcePackStatus.valueOf(result.name()), false, false);
        Bukkit.getPluginManager().callEvent(packEvent);
        event.setCancelled(packEvent.isCancelled());
    }

    private void handlePluginMessage(PacketReceiveEvent event) {
        if (!plugin.velocityMode) return;

        final String channel;
        final byte[] data;
        if (event.getPacketType() == PacketType.Configuration.Client.PLUGIN_MESSAGE) {
            final WrapperConfigClientPluginMessage wrapper = new WrapperConfigClientPluginMessage(event);
            channel = wrapper.getChannelName();
            data = wrapper.getData();
        } else {
            final WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);
            channel = wrapper.getChannelName();
            data = wrapper.getData();
        }

        if (!STATUS_CHANNEL.equals(channel)) return;

        // The proxy sends us this on a custom channel - don't let it propagate any further.
        event.setCancelled(true);

        final User user = event.getUser();
        final ForcePackPaperPlayer player = plugin.getForcePackPlayer(user.getUUID()).orElse(null);
        if (player == null) {
            // Player isn't valid - wasn't added to waiting (exempt or not yet tracked)
            return;
        }

        final String[] split = new String(data, StandardCharsets.UTF_8).split(";");
        plugin.log("Posted event");

        final ResourcePackStatus status = ResourcePackStatus.valueOf(split[1]);
        final UUID packId = UUID.fromString(split[0]);
        final boolean proxyRemove = Boolean.parseBoolean(split[2]);
        final GameProfile gameProfile = new GameProfile(user.getUUID(), user.getName());
        Bukkit.getPluginManager().callEvent(new MultiVersionResourcePackStatusEvent(gameProfile, packId, status, true, proxyRemove));
    }

    private boolean isResourcePackStatus(PacketTypeCommon type) {
        return type == PacketType.Play.Client.RESOURCE_PACK_STATUS || type == PacketType.Configuration.Client.RESOURCE_PACK_STATUS;
    }

    private boolean isPluginMessage(PacketTypeCommon type) {
        return type == PacketType.Play.Client.PLUGIN_MESSAGE || type == PacketType.Configuration.Client.PLUGIN_MESSAGE;
    }
}
