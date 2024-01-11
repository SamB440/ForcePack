package com.convallyria.forcepack.velocity.listener;

import com.convallyria.forcepack.api.check.SpoofCheck;
import com.convallyria.forcepack.api.permission.Permissions;
import com.convallyria.forcepack.api.player.ForcePackPlayer;
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
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import net.kyori.adventure.text.Component;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ResourcePackListener {

    private final ForcePackVelocity plugin;

    public ResourcePackListener(final ForcePackVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        final Player player = event.getPlayer();
        final Optional<ServerConnection> currentServer = player.getCurrentServer();
        if (currentServer.isEmpty()) {
            plugin.log(player.getUsername() + "'s server does not exist.");
            return;
        }

        final PlayerResourcePackStatusEvent.Status status = event.getStatus();

        // Check if the server they're on has a resource pack
        final String serverName = currentServer.get().getServerInfo().getName();
        final ResourcePackInfo packInfo = event.getPackInfo(); // Returns null on < 1.20.3 clients, and if a UUID isn't provided I guess?
        final UUID id = packInfo == null ? null : packInfo.getId();
        if (id != null) plugin.log(player.getUsername() + " sent response id '%s'", id.toString());

        if ((packInfo != null && packInfo.getOrigin() != ResourcePackInfo.Origin.PLUGIN_ON_PROXY) || !plugin.getPackHandler().isWaitingFor(player, id)) {
            plugin.log("Resource pack with URL %s and ID %s was sent from a downstream server! This is unsupported behaviour.", packInfo == null ? "(unknown: legacy)" : packInfo.getUrl(), id);
            return;
        }

        // If on 1.20.3+, use UUID to find the resource pack
        // Otherwise, it's a singular resource pack, just filter to what is the first (and can only be) the first element.
        final Set<ResourcePack> packsByServer = plugin.getPacksByServerAndVersion(serverName, player.getProtocolVersion()).orElse(null);
        ResourcePack packByServer = packsByServer == null ? null : packsByServer.stream()
                .filter(pack -> player.getProtocolVersion().getProtocol() < ProtocolVersion.MINECRAFT_1_20_3.getProtocol() || pack.getUUID().equals(id))
                .findFirst().orElse(null);
        if (packByServer == null) {
            plugin.log("%s does not have a resource pack matching %s, ignoring status %s.", serverName, id == null ? "null" : id.toString(), status.toString());
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

        if (tryValidateHacks(player, status, root)) return;

        final VelocityConfig actions = root.getConfig("actions").getConfig(status.name());
        if (actions != null) {
            for (String cmd : actions.getStringList("commands")) {
                final CommandSource console = plugin.getServer().getConsoleCommandSource();
                plugin.getServer().getCommandManager().executeAsync(console, cmd.replace("[player]", player.getUsername()));
            }
        }

        final boolean kick = actions != null && actions.getBoolean("kick");

        // Declined/failed is valid and should be allowed, server owner decides whether they get kicked
        if (status != PlayerResourcePackStatusEvent.Status.ACCEPTED && status != PlayerResourcePackStatusEvent.Status.DOWNLOADED && !kick) {
            if (status == PlayerResourcePackStatusEvent.Status.SUCCESSFUL) {
                plugin.getOrCreatePlayer(player).addAppliedPack(plugin.getServer().createResourcePackBuilder(packByServer.getURL()).setId(packByServer.getUUID()).setHash(packByServer.getHashSum()).build());
            }
            plugin.log("Sent player '%s' plugin message downstream to '%s' for status '%s'", player.getUsername(), currentServer.get().getServerInfo().getName(), status.name());
            // No longer applying, remove them from the list
            plugin.getPackHandler().processWaitingResourcePack(player, packByServer.getUUID());
            final String name = status == PlayerResourcePackStatusEvent.Status.SUCCESSFUL ? "SUCCESSFULLY_LOADED" : status.name();
            currentServer.get().sendPluginMessage(PackHandler.FORCEPACK_STATUS_IDENTIFIER, (packByServer.getUUID().toString() + ";" + name + ";" + !plugin.getPackHandler().isWaiting(player)).getBytes(StandardCharsets.UTF_8));
        }

        final String text = actions == null ? null : actions.getString("message");
        if (text == null) return;

        final Component component = plugin.getMiniMessage().deserialize(text);
        if (kick) {
            player.disconnect(component);
        } else {
            player.sendMessage(component);
        }
    }

    private boolean tryValidateHacks(Player player, PlayerResourcePackStatusEvent.Status status, VelocityConfig root) {
        final boolean tryPrevent = plugin.getConfig().getBoolean("try-to-stop-fake-accept-hacks", true);
        if (!tryPrevent) return false;

        final ForcePackPlayer forcePackPlayer = plugin.getPackHandler().getForcePackPlayer(player).orElse(null);
        if (forcePackPlayer == null) {
            plugin.log("Not checking " + player.getUsername() + " because they are not in waiting.");
            return false;
        }

        boolean hasFailed = false;
        for (SpoofCheck check : forcePackPlayer.getChecks()) {
            final SpoofCheck.CheckStatus checkStatus = check.receiveStatus(status.name(), plugin::log);
            hasFailed = checkStatus == SpoofCheck.CheckStatus.FAILED;
            if (checkStatus == SpoofCheck.CheckStatus.CANCEL) {
                plugin.log("Cancelling status " + status + " as a check requested it.");
                return true;
            }
        }

        if (hasFailed) {
            plugin.log("Kicking player " + player.getUsername() + " because they failed a check.");
            final VelocityConfig actionsRoot = root.getConfig("actions");
            final VelocityConfig actions = actionsRoot.getConfig("FAILED_DOWNLOAD");
            return disconnectAction(player, actions);
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
        plugin.removePlayer(player);
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
