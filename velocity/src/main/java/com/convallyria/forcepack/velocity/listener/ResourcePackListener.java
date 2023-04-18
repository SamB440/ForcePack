package com.convallyria.forcepack.velocity.listener;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ResourcePackListener {

    private final ForcePackVelocity plugin;
    private final Map<UUID, Long> sentAccept = new HashMap<>();

    public ResourcePackListener(final ForcePackVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        final long now = System.currentTimeMillis();
        final Player player = event.getPlayer();
        final Optional<ServerConnection> currentServer = player.getCurrentServer();
        if (currentServer.isEmpty()) {
            plugin.log(player.getUsername() + "'s server does not exist.");
            return;
        }

        // Check if the server they're on has a resource pack
        final String serverName = currentServer.get().getServerInfo().getName();
        final Optional<ResourcePack> packByServer = plugin.getPackByServer(serverName);
        if (packByServer.isEmpty()) {
            plugin.log(serverName + " does not have a ResourcePack, ignoring.");
            return;
        }

        final PlayerResourcePackStatusEvent.Status status = event.getStatus();
        boolean geyser = plugin.getConfig().getBoolean("geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        boolean canBypass = player.hasPermission("ForcePack.bypass") && plugin.getConfig().getBoolean("bypass-permission");
        if (!canBypass && !geyser) {
            if (status == PlayerResourcePackStatusEvent.Status.SUCCESSFUL) {
                // No longer applying, remove them from the list
                currentServer.get().sendPluginMessage(MinecraftChannelIdentifier.create("forcepack", "status"), "SUCCESSFULLY_LOADED".getBytes(StandardCharsets.UTF_8));
                plugin.getPackHandler().getApplying().remove(player.getUniqueId());
            }
            plugin.log(player.getUsername() + " sent status: " + event.getStatus());

            final VelocityConfig root;
            if (packByServer.get().getServer().equals(ForcePackVelocity.GLOBAL_SERVER_NAME)) {
                root = plugin.getConfig().getConfig("global-pack");
            } else {
                root = plugin.getConfig().getConfig("servers").getConfig(serverName);
            }

            if (tryValidateHacks(player, status, root, now)) return;
            if (status != PlayerResourcePackStatusEvent.Status.ACCEPTED) sentAccept.remove(player.getUniqueId());

            final VelocityConfig actions = root.getConfig("actions").getConfig(status.name());
            for (String cmd : actions.getStringList("commands")) {
                final CommandSource console = plugin.getServer().getConsoleCommandSource();
                plugin.getServer().getCommandManager().executeAsync(console, cmd);
            }

            final boolean kick = actions.getBoolean("kick");
            final String text = actions.getString("message");
            if (text == null) return;

            final Component component = plugin.getMiniMessage().deserialize(text);
            if (kick) {
                player.disconnect(component);
            } else {
                player.sendMessage(component);
            }
        } else {
            plugin.log("Ignoring player " + player.getUsername() + " as they do not have permissions or are a geyser player.");
        }
    }

    private boolean tryValidateHacks(Player player, PlayerResourcePackStatusEvent.Status status, VelocityConfig root, long now) {
        final boolean tryPrevent = plugin.getConfig().getBoolean("try-to-stop-fake-accept-hacks", true);
        if (!tryPrevent) return false;

        final VelocityConfig actionsRoot = root.getConfig("actions");
        if (status == PlayerResourcePackStatusEvent.Status.ACCEPTED) {
            if (sentAccept.containsKey(player.getUniqueId())) {
                plugin.log("Kicked player " + player.getUsername() + " because they are sending fake resource pack statuses (accepted sent twice).");
                final VelocityConfig actions = actionsRoot.getConfig("DECLINED");
                return disconnectAction(player, actions);
            }
            sentAccept.put(player.getUniqueId(), now);
        } else if (status == PlayerResourcePackStatusEvent.Status.SUCCESSFUL) {
            if (!sentAccept.containsKey(player.getUniqueId())) {
                plugin.log("Kicked player " + player.getUsername() + " because they are sending fake resource pack statuses (order not maintained).");
                final VelocityConfig actions = actionsRoot.getConfig("FAILED_DOWNLOAD");
                return disconnectAction(player, actions);
            }

            long time = now - sentAccept.remove(player.getUniqueId());
            if (time <= 10) {
                plugin.log("Kicked player " + player.getUsername() + " because they are sending fake resource pack statuses (sent too fast).");
                final VelocityConfig actions = actionsRoot.getConfig("FAILED_DOWNLOAD");
                return disconnectAction(player, actions);
            }
        }
        return false;
    }

    private boolean disconnectAction(Player player, VelocityConfig actions) {
        final String text = actions.getString("message");
        if (text == null) return true;
        player.disconnect(plugin.getMiniMessage().deserialize(text));
        return true;
    }

    @Subscribe
    public void onQuit(DisconnectEvent event) {
        final Player player = event.getPlayer();
        sentAccept.remove(player.getUniqueId());
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onJoin(ServerPostConnectEvent event) {
        final Player player = event.getPlayer();
        final Optional<ServerConnection> currentServer = player.getCurrentServer();
        if (currentServer.isEmpty()) return;

        boolean geyser = plugin.getConfig().getBoolean("geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        boolean canBypass = player.hasPermission("ForcePack.bypass") && plugin.getConfig().getBoolean("bypass-permission");
        plugin.log(player.getUsername() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");
        if (!canBypass && !geyser) {
            plugin.getPackHandler().setPack(player, currentServer.get());
        }
    }
}
