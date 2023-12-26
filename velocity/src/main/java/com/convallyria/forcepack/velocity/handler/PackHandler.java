package com.convallyria.forcepack.velocity.handler;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class PackHandler {

    public static final MinecraftChannelIdentifier FORCEPACK_STATUS_IDENTIFIER = MinecraftChannelIdentifier.create("forcepack", "status");

    private final ForcePackVelocity plugin;
    private final Map<UUID, Set<ResourcePack>> waiting;

    public PackHandler(final ForcePackVelocity plugin) {
        this.plugin = plugin;
        this.waiting = new HashMap<>();
    }

    public void processWaitingResourcePack(Player player, UUID packId) {
        final UUID playerId = player.getUniqueId();
        // If the player is on a version older than 1.20.3, they can only have one resource pack.
        if (player.getProtocolVersion().getProtocol() < ProtocolVersion.MINECRAFT_1_20_3.getProtocol()) {
            removeFromWaiting(player);
            return;
        }

        final Set<ResourcePack> newSet = waiting.computeIfPresent(playerId, (a, packs) -> {
            packs.removeIf(pack -> pack.getUUID().equals(packId));
            return packs;
        });

        if (newSet == null || newSet.isEmpty()) {
            removeFromWaiting(player);
        }
    }

    public boolean isWaiting(Player player) {
        return waiting.containsKey(player.getUniqueId());
    }

    public boolean isWaitingFor(Player player, @Nullable UUID packId) {
        if (!isWaiting(player)) return false;

        // If the player is on a version older than 1.20.3, they can only have one resource pack.
        if (player.getProtocolVersion().getProtocol() < ProtocolVersion.MINECRAFT_1_20_3.getProtocol() || packId == null) {
            return true;
        }

        final Set<ResourcePack> waitingPacks = waiting.get(player.getUniqueId());
        return waitingPacks.stream().anyMatch(pack -> pack.getUUID().equals(packId));
    }

    public Set<ResourcePack> getWaitingFor(Player player) {
        return waiting.getOrDefault(player.getUniqueId(), Set.of());
    }

    public void removeFromWaiting(Player player) {
        waiting.remove(player.getUniqueId());
    }

    public void addToWaiting(UUID uuid, @Nullable Set<ResourcePack> packs) {
        waiting.compute(uuid, (a, existing) -> {
            if (existing != null && packs != null) {
                existing.addAll(packs);
                return existing;
            }
            return packs == null ? null : new HashSet<>(packs);
        });
    }

    public void setPack(final Player player, final ServerConnection server) {
        // Find whether the config contains this server
        final ServerInfo serverInfo = server.getServerInfo();
        final ProtocolVersion protocolVersion = player.getProtocolVersion();
        final int protocol = protocolVersion.getProtocol();
        plugin.getPacksByServerAndVersion(serverInfo.getName(), protocolVersion).ifPresentOrElse(resourcePacks -> {
            final boolean forceApply = protocol != ProtocolVersion.MINECRAFT_1_20_2.getProtocol() && plugin.getConfig().getBoolean("force-constant-download", false);
            if (protocol >= ProtocolVersion.MINECRAFT_1_20_3.getProtocol()) {
                // Get all packs the player has applied and remove any not on this server!
                player.getAppliedResourcePacks().stream().filter(pack -> forceApply || resourcePacks.stream().noneMatch(pack2 -> pack2.getUUID().equals(pack.getId()))).forEach(toRemove -> {
                    player.removeResourcePacks(toRemove.getId());
                });
            }

            final int maxSize = ClientVersion.getMaxSizeForVersion(protocol);
            final boolean forceSend = plugin.getConfig().getBoolean("force-invalid-size", false);

            // Get all packs that need to be applied
            resourcePacks.stream().filter(pack -> {
                if (forceApply) return true; // force = all packs are sent

                // Check if they already have this ResourcePack applied.
                for (ResourcePackInfo applied : player.getAppliedResourcePacks()) {
                    if (Arrays.equals(applied.getHash(), pack.getHashSum())) {
                        plugin.log("Not applying already applied pack '" + pack.getUUID().toString() + "' to player " + player.getUsername() + ".");
                        server.sendPluginMessage(FORCEPACK_STATUS_IDENTIFIER, (pack.getUUID().toString() + ";SUCCESSFULLY_LOADED").getBytes(StandardCharsets.UTF_8));
                        return false;
                    }
                }

                if (!forceSend && pack.getSize() > maxSize) {
                    plugin.log(String.format("Not sending pack %s to %s because of excessive size for version %d (%dMB, %dMB).", pack.getUUID().toString(), player.getUsername(), protocol, pack.getSize(), maxSize));
                    return false;
                }

                return true;
            }).forEach(toApply -> this.runSetPackTask(player, toApply, protocol));
        }, () -> {
            // 1.20.3+ allows us to simply clear all their applied resource packs!
            if (protocol >= ProtocolVersion.MINECRAFT_1_20_3.getProtocol()) {
                player.clearResourcePacks();
                return;
            }

            final ResourcePackInfo appliedResourcePack = player.getAppliedResourcePack();
            // This server doesn't have a pack set - send unload pack if enabled and if they already have one
            if (appliedResourcePack == null) {
                plugin.log("%s doesn't have a resource pack applied, not sending unload.", player.getUsername());
                return;
            }

            final VelocityConfig unloadPack = plugin.getConfig().getConfig("unload-pack");
            final boolean enableUnload = unloadPack.getBoolean("enable");
            if (!enableUnload) {
                plugin.log("Unload pack is disabled, not sending for server %s, user %s.", serverInfo.getName(), player.getUsername());
                return;
            }

            final List<String> excluded = unloadPack.getStringList("exclude");
            if (excluded.contains(serverInfo.getName())) return;

            plugin.getPacksByServerAndVersion(ForcePackVelocity.EMPTY_SERVER_NAME, player.getProtocolVersion()).ifPresent(packs -> {
                // This can only return 1 pack since at this point the player is <= 1.20.2
                for (ResourcePack empty : packs) {
                    // If their current applied resource pack is the unloaded one, don't send it again
                    // Checking URL rather than hash should be fine... it's simpler and should be unique.
                    if (appliedResourcePack.getUrl().equals(empty.getURL())) return;

                    empty.setResourcePack(player.getUniqueId());
                }
            });
        });
    }

    private void runSetPackTask(Player player, ResourcePack resourcePack, int protocol) {
        // There is a bug in velocity when connecting to another server, where the prompt screen
        // will be forcefully closed by the server if we don't delay it for a second.
        final boolean update = plugin.getConfig().getBoolean("update-gui", true);
        AtomicReference<ScheduledTask> task = new AtomicReference<>();
        final Scheduler.TaskBuilder builder = plugin.getServer().getScheduler().buildTask(plugin, () -> {
            for (ResourcePackInfo appliedResourcePack : player.getAppliedResourcePacks()) {
                // Check the pack they have applied now is the one we're looking for.
                if (Arrays.equals(appliedResourcePack.getHash(), resourcePack.getHashSum())) {
                    if (task.get() != null) task.get().cancel();
                }
            }

            plugin.log("Applying resource pack " + resourcePack.getUUID().toString() + " to " + player.getUsername() + ".");
            resourcePack.setResourcePack(player.getUniqueId());
        }).delay(1L, TimeUnit.SECONDS);

        if (update && protocol <= 340) { // Prevent escaping out for clients on <= 1.12
            final long speed = plugin.getConfig().getLong("update-gui-speed", 1000);
            builder.repeat(speed, TimeUnit.MILLISECONDS);
        }

        addToWaiting(player.getUniqueId(), Set.of(resourcePack));
        task.set(builder.schedule());
    }
}
