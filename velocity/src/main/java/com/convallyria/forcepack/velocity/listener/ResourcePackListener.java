package com.convallyria.forcepack.velocity.listener;

import com.convallyria.forcepack.api.permission.Permissions;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.convallyria.forcepack.velocity.handler.PackHandler;
import com.convallyria.forcepack.velocity.resourcepack.VelocityResourcePack;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

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

        final PlayerResourcePackStatusEvent.Status status = event.getStatus();

        // Check if the server they're on has a resource pack
        final String serverName = currentServer.get().getServerInfo().getName();
        final ResourcePack packByServer = plugin.getPackByServerAndVersion(serverName, player.getProtocolVersion()).orElse(null);
        if (packByServer == null) {
            plugin.log("%s does not have a resource pack, ignoring status %s.", serverName, status.toString());
            return;
        }

        boolean geyser = plugin.getConfig().getBoolean("geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        boolean canBypass = player.hasPermission(Permissions.BYPASS) && plugin.getConfig().getBoolean("bypass-permission");
        if (canBypass || geyser) {
            plugin.log("Ignoring player " + player.getUsername() + " as they do not have permissions or are a geyser player.");
            return;
        }

        final VelocityConfig root;
        if (packByServer.getServer().equals(ForcePackVelocity.GLOBAL_SERVER_NAME)) {
            root = plugin.getConfig().getConfig("global-pack");
        } else {
            if (packByServer instanceof VelocityResourcePack) {
                VelocityResourcePack vrp = (VelocityResourcePack) packByServer;
                if (vrp.getGroup() != null) {
                    root = plugin.getConfig().getConfig("groups").getConfig(vrp.getGroup());
                } else {
                    root = plugin.getConfig().getConfig("servers").getConfig(serverName);
                }
            } else {
                root = plugin.getConfig().getConfig("servers").getConfig(serverName);
            }
        }

        plugin.log(player.getUsername() + " sent status: " + event.getStatus());

        if (tryValidateHacks(player, status, root, now)) return;

        final VelocityConfig actions = root.getConfig("actions").getConfig(status.name());
        if (actions != null) {
            for (String cmd : actions.getStringList("commands")) {
                final CommandSource console = plugin.getServer().getConsoleCommandSource();
                plugin.getServer().getCommandManager().executeAsync(console, cmd.replace("[player]", player.getUsername()));
            }
        }

        final boolean kick = actions != null && actions.getBoolean("kick");

        // Declined/failed is valid and should be allowed, server owner decides whether they get kicked
        if (status != PlayerResourcePackStatusEvent.Status.ACCEPTED && !kick) {
            plugin.log("Sent player '%s' plugin message downstream to '%s' for status '%s'", player.getUsername(), currentServer.get().getServerInfo().getName(), status.name());
            // No longer applying, remove them from the list
            final String name = status == PlayerResourcePackStatusEvent.Status.SUCCESSFUL ? "SUCCESSFULLY_LOADED" : status.name();
            currentServer.get().sendPluginMessage(PackHandler.FORCEPACK_STATUS_IDENTIFIER, (packByServer.getUUID().toString() + ";" + name).getBytes(StandardCharsets.UTF_8));
            plugin.getPackHandler().getApplying().remove(player.getUniqueId());
        }

        if (status != PlayerResourcePackStatusEvent.Status.ACCEPTED) sentAccept.remove(player.getUniqueId());

        final String text = actions == null ? null : actions.getString("message");
        if (text == null) return;

        final Component component = plugin.getMiniMessage().deserialize(text);
        if (kick) {
            player.disconnect(component);
        } else {
            player.sendMessage(component);
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
        boolean canBypass = player.hasPermission(Permissions.BYPASS) && plugin.getConfig().getBoolean("bypass-permission");
        plugin.log(player.getUsername() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");
        if (!canBypass && !geyser) {
            plugin.getPackHandler().setPack(player, currentServer.get());
        }
    }
}
