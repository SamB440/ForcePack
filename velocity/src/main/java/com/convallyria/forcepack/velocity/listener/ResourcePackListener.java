package com.convallyria.forcepack.velocity.listener;

import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public record ResourcePackListener(ForcePackVelocity plugin) {

    @Subscribe(order = PostOrder.EARLY)
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        final Player player = event.getPlayer();
        final Optional<ServerConnection> currentServer = player.getCurrentServer();
        if (currentServer.isEmpty()) {
            plugin.log(player.getUsername() + "'s server does not exist.");
            return;
        }

        // Check if the server they're on has a resource pack
        final String serverName = currentServer.get().getServerInfo().getName();
        if (plugin.getPackByServer(serverName).isEmpty()) {
            plugin.log(serverName + " does not have a ResourcePack, ignoring.");
            return;
        }

        final PlayerResourcePackStatusEvent.Status status = event.getStatus();
        boolean geyser = plugin.getConfig().getBoolean("geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        if (!player.hasPermission("ForcePack.bypass") && !geyser) {
            plugin.log(player.getUsername() + " sent status: " + event.getStatus());

            final VelocityConfig actions = plugin.getConfig().getConfig("servers").getConfig(serverName).getConfig("actions").getConfig(status.name());
            for (String cmd : actions.getStringList("commands")) {
                final CommandSource console = plugin.getServer().getConsoleCommandSource();
                plugin.getServer().getCommandManager().executeAsync(console, cmd);
            }

            final boolean kick = actions.getBoolean("kick");
            final String text = actions.getString("message");
            if (text == null) return;

            final Component component = plugin.getMiniMessage().parse(text);
            if (kick) {
                player.disconnect(component);
            } else {
                player.sendMessage(component);
            }
        } else {
            plugin.log("Ignoring player " + player.getUsername() + " as they do not have permissions or are a geyser player.");
        }
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onJoin(ServerPostConnectEvent event) {
        final Player player = event.getPlayer();
        final Optional<ServerConnection> currentServer = player.getCurrentServer();
        if (currentServer.isEmpty()) return;

        // Find whether the config contains this server
        final ServerInfo serverInfo = currentServer.get().getServerInfo();

        boolean geyser = plugin.getConfig().getBoolean("geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        if (!player.hasPermission("ForcePack.bypass") && !geyser) {
            plugin.getPackByServer(serverInfo.getName()).ifPresentOrElse(resourcePack -> {
                // Check if they already have this ResourcePack applied.
                final ResourcePackInfo appliedResourcePack = player.getAppliedResourcePack();
                if (appliedResourcePack != null) {
                    if (Arrays.equals(appliedResourcePack.getHash(), resourcePack.getHashSum())) {
                        plugin.log("Not applying already applied pack to player " + player.getUsername() + ".");
                        return;
                    }
                }

                // With velocity, we don't actually need to schedule a task. The proxy handles this correctly unlike Spigot.
                // If they escape out, it will detect it as the denied status.
                // Velocity also queues the requests, so if we used a task it would make them accept more resource packs
                // the longer you would be in the prompt screen.
                // However, there is also a bug in velocity when connecting to another server, where the prompt screen
                // will be forcefully closed by the server if we don't delay it for a second.
                plugin.getServer().getScheduler().buildTask(plugin, () -> {
                    plugin.log("Applying ResourcePack to " + player.getUsername() + ".");
                    resourcePack.setResourcePack(player.getUniqueId());
                }).delay(1L, TimeUnit.SECONDS).schedule();
            }, () -> {
                // This server doesn't have a pack set - send unload pack if enabled and if they already have one
                if (player.getAppliedResourcePack() == null) return;
                final VelocityConfig unloadPack = plugin.getConfig().getConfig("unload-pack");
                final boolean enableUnload = unloadPack.getBoolean("enable");
                if (!enableUnload) return;
                plugin.getPackByServer(ForcePackVelocity.EMPTY_SERVER_NAME).ifPresent(empty -> empty.setResourcePack(player.getUniqueId()));
            });
        }
    }
}
