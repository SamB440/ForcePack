package com.convallyria.forcepack.velocity.handler;

import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.server.ServerInfo;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public final record PackHandler(ForcePackVelocity plugin) {

    public void setPack(final Player player, final ServerInfo serverInfo) {
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
