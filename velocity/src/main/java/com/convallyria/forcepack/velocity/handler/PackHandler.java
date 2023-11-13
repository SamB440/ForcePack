package com.convallyria.forcepack.velocity.handler;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.convallyria.forcepack.webserver.ForcePackWebServer;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.api.scheduler.Scheduler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class PackHandler {

    public static final MinecraftChannelIdentifier FORCEPACK_STATUS_IDENTIFIER = MinecraftChannelIdentifier.create("forcepack", "status");

    private final ForcePackVelocity plugin;
    private final List<UUID> applying;
    private final Map<UUID, Consumer<Void>> sentFalsePack = new HashMap<>();

    public PackHandler(final ForcePackVelocity plugin) {
        this.plugin = plugin;
        this.applying = new ArrayList<>();
    }

    public List<UUID> getApplying() {
        return applying;
    }

    public Map<UUID, Consumer<Void>> getSentFalsePack() {
        return sentFalsePack;
    }

    public void setPack(final Player player, final ServerConnection server) {
        // Find whether the config contains this server
        final ServerInfo serverInfo = server.getServerInfo();
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
            final boolean forceApply = (plugin.getConfig().getBoolean("ignore-1-20-2-server-switch-players", true) && player.getProtocolVersion().getProtocol() < ProtocolVersion.MINECRAFT_1_20_2.getProtocol()) && plugin.getConfig().getBoolean("force-constant-download", false);
            if (appliedResourcePack != null && !forceApply) {
                if (Arrays.equals(appliedResourcePack.getHash(), resourcePack.getHashSum())) {
                    plugin.log("Not applying already applied pack to player " + player.getUsername() + ".");
                    server.sendPluginMessage(FORCEPACK_STATUS_IDENTIFIER, "SUCCESSFULLY_LOADED".getBytes(StandardCharsets.UTF_8));
                    return;
                }
            }

            final boolean tryPrevent = plugin.getConfig().getBoolean("try-to-stop-fake-accept-hacks", true);
            if (tryPrevent) {
                sendFalsePack(player, resourcePack, protocol);
            } else {
                runSetPackTask(player, resourcePack, protocol);
            }
        }, () -> {
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

            plugin.getPackByServer(ForcePackVelocity.EMPTY_SERVER_NAME).ifPresent(empty -> {
                // If their current applied resource pack is the unloaded one, don't send it again
                // Checking URL rather than hash should be fine... it's simpler and should be unique.
                if (appliedResourcePack.getUrl().equals(empty.getURL())) return;

                empty.setResourcePack(player.getUniqueId());
            });
        });
    }

    private void sendFalsePack(Player player, ResourcePack pack, int version) {
        final Optional<ForcePackWebServer> packWebServer = plugin.getWebServer();
        final String fakeFile = UUID.randomUUID().toString().replace("-", "") + ".zip";
        final String fakeUrl = packWebServer.map(server -> server.getUrl() + "/serve/" + fakeFile).orElse("https://");
        packWebServer.ifPresent(server -> server.awaitServe(fakeFile, () -> {
            if (!player.isActive()) return;
            // The player has requested the resource pack.
            // So, we can send a close inventory packet along with the response.
            // This prevents the "pack application failed" screen from showing.
            // By sending it along with the pack response, we prevent slow connections seeing the screen for longer,
            //  as theoretically these packets should arrive at about the same time.
            //  (as long as the server is not lagging and depending on when the packets get flushed)
            // Most people won't notice it at all unless they are paying attention.
//            PacketEvents.getAPI().getPlayerManager().getUser(player).writePacket(new WrapperPlayServerCloseWindow(0));
        }));

        plugin.log("Sending fake resource pack '" + fakeUrl + "' to " + player.getUsername() + ".");
        final ResourcePackInfo.Builder infoBuilder = plugin.getServer()
                .createResourcePackBuilder(fakeUrl)
                .setHash(UUID.randomUUID().toString().substring(0, 20).getBytes(StandardCharsets.UTF_8))
                .setShouldForce(plugin.getConfig().getBoolean("use-new-force-pack-screen", true));
        player.sendResourcePackOffer(infoBuilder.build());

        sentFalsePack.put(player.getUniqueId(), (v) -> {
//            PacketEvents.getAPI().getPlayerManager().getUser(player).writePacket(new WrapperPlayServerCloseWindow(0));
            this.runSetPackTask(player, pack, version);
        });
    }

    private void runSetPackTask(Player player, ResourcePack resourcePack, int protocol) {
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
    }
}
