package com.convallyria.forcepack.velocity.handler;

import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class PackHandler {

    private final ForcePackVelocity plugin;
    private final List<UUID> applying;

    public PackHandler(final ForcePackVelocity plugin) {
        this.plugin = plugin;
        this.applying = new ArrayList<>();
    }

    public List<UUID> getApplying() {
        return applying;
    }

    public void setPack(final Player player, final ServerInfo serverInfo) {
        plugin.getPackByServer(serverInfo.getName()).ifPresentOrElse(resourcePack -> {
            final int protocol = player.getProtocolVersion().getProtocol();
            final int maxSize = ClientVersion.getMaxSizeForVersion(protocol);
            final boolean forceSend = plugin.getConfig().getBoolean("force-invalid-size", false);
            if (!forceSend && resourcePack.getSize() > maxSize) {
                plugin.log(String.format("Not sending pack to %s because of excessive size for version %d (%dMB, %dMB).", player.getUsername(), protocol, resourcePack.getSize(), maxSize));
                return;
            }

            // Check if they already have this ResourcePack applied.
            final ResourcePackInfo appliedResourcePack = player.getAppliedResourcePack();
            final boolean forceApply = plugin.getConfig().getBoolean("force-constant-download", false);
            if (appliedResourcePack != null && !forceApply) {
                if (Arrays.equals(appliedResourcePack.getHash(), resourcePack.getHashSum())) {
                    plugin.log("Not applying already applied pack to player " + player.getUsername() + ".");
                    return;
                }
            }

            // There is a bug in velocity when connecting to another server, where the prompt screen
            // will be forcefully closed by the server if we don't delay it for a second.
            final boolean update = plugin.getConfig().getBoolean("update-gui", true);
            AtomicReference<ScheduledTask> task = new AtomicReference<>();
            final Scheduler.TaskBuilder builder = plugin.getServer().getScheduler().buildTask(plugin, () -> {
                if (player.getAppliedResourcePack() != null) {
                    // Check the pack they have applied now is the one we're looking for.
                    if (Arrays.equals(player.getAppliedResourcePack().getHash(), resourcePack.getHashSum())) {
                       if (task.get() != null) task.get().cancel();
                    }
                }
                plugin.log("Applying ResourcePack to " + player.getUsername() + ".");
                resourcePack.setResourcePack(player.getUniqueId());
            }).delay(1L, TimeUnit.SECONDS);
            if (update && protocol <= 340) { // Prevent escaping out for clients on <= 1.12
                final long speed = plugin.getConfig().getLong("update-gui-speed", 1000);
                builder.repeat(speed, TimeUnit.MILLISECONDS);
            }
            applying.add(player.getUniqueId());
            task.set(builder.schedule());
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
