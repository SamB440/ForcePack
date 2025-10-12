package com.convallyria.forcepack.sponge.listener;

import com.convallyria.forcepack.api.check.SpoofCheck;
import com.convallyria.forcepack.api.permission.Permissions;
import com.convallyria.forcepack.api.player.ForcePackPlayer;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.schedule.PlatformScheduler;
import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.sponge.ForcePackSponge;
import com.convallyria.forcepack.sponge.event.ForcePackReloadEvent;
import com.convallyria.forcepack.sponge.event.MultiVersionResourcePackStatusEvent;
import com.convallyria.forcepack.sponge.util.ProtocolUtil;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.living.player.ResourcePackStatusEvent;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class ResourcePackListener {

    private final ForcePackSponge plugin;

    private final Map<UUID, Long> sentAccept = new ConcurrentHashMap<>();

    public ResourcePackListener(final ForcePackSponge plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onStatus(MultiVersionResourcePackStatusEvent event) {
        final ServerPlayer player = event.getPlayer();
        final UUID id = event.getID();

        boolean geyser = getConfig().node("Server", "geyser").getBoolean() && GeyserUtil.isBedrockPlayer(player.uniqueId());
        boolean canBypass = player.hasPermission(Permissions.BYPASS) && getConfig().node("Server", "bypass-permission").getBoolean();
        plugin.log(player.name() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");

        if (canBypass || geyser) {
            return;
        }

        if (plugin.temporaryExemptedPlayers.remove(player.uniqueId())) {
            plugin.log("Ignoring player " + player.name() + " as they have a one-off exemption.");
            return;
        }

        final ResourcePackStatus status = event.getStatus();
        plugin.log(player.name() + " sent status: " + status);

        final boolean velocityMode = getConfig().node("velocity-mode").getBoolean();
        if (!velocityMode && tryValidateHacks(player, event, status)) return;

        // If we did not set this resource pack, ignore
        if (!event.isProxy() && !plugin.isWaitingFor(player, id)) {
            plugin.log("Ignoring resource pack " + id + " because it wasn't set by ForcePack.");
            return;
        } else if (event.isProxy()) {
            plugin.log("Resource pack with id " + id + " sent by proxy. Removal state: " + event.isProxyRemove());
        }

        // Only remove from waiting if they actually loaded the resource pack, rather than any status
        // Declined/failed is valid and should be allowed, server owner decides whether they get kicked
        if (status != ResourcePackStatus.ACCEPTED && status != ResourcePackStatus.DOWNLOADED) {
            if (event.isProxy()) {
                if (event.isProxyRemove()) {
                    plugin.removeFromWaiting(player);
                }
            } else {
                plugin.processWaitingResourcePack(player, id);
            }
        }

        try {
            for (String cmd : getConfig().node("Server", "Actions", status.name(), "Commands").getList(String.class, new ArrayList<>())) {
                ensureMainThread(() -> {
                    try {
                        Sponge.server().commandManager().process(cmd.replace("[player]", player.name()));
                    } catch (CommandException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        // Don't execute kicks - handled by proxy
        if (velocityMode) {
            plugin.getScheduler().executeOnMain(() -> this.callSpongeEvent(event));
            return;
        }

        final boolean kick = getConfig().node("Server", "Actions", status.name(), "kick").getBoolean();

        switch (status) {
            case ACCEPTED: {
                sentAccept.put(player.uniqueId(), System.currentTimeMillis());
                break;
            }
            case DECLINED: {
                ensureMainThread(() -> {
                    if (kick) player.kick(Component.translatable("forcepack.declined"));
                    else player.sendMessage(Component.translatable("forcepack.declined"));
                });

                sentAccept.remove(player.uniqueId());
                break;
            }
            case DISCARDED:
            case INVALID_URL:
            case FAILED_RELOAD:
            case FAILED_DOWNLOAD: {
                ensureMainThread(() -> {
                    if (kick) player.kick(Component.translatable("forcepack.download_failed"));
                    else player.sendMessage(Component.translatable("forcepack.download_failed"));
                });

                sentAccept.remove(player.uniqueId());
                break;
            }
            case SUCCESSFULLY_LOADED: {
                if (kick) ensureMainThread(() -> player.kick(Component.translatable("forcepack.accepted")));
                else {
                    ensureMainThread(() -> player.sendMessage(Component.translatable("forcepack.accepted")));
                    boolean sendTitle = plugin.getConfig().node("send-loading-title").getBoolean();
                    if (sendTitle) player.clearTitle();
                }
                break;
            }
        }
    }

    private boolean tryValidateHacks(ServerPlayer player, MultiVersionResourcePackStatusEvent event, ResourcePackStatus status) {
        final boolean tryPrevent = getConfig().node("try-to-stop-fake-accept-hacks").getBoolean(true);
        if (!tryPrevent) return false;

        final ForcePackPlayer forcePackPlayer = plugin.getForcePackPlayer(player).orElse(null);
        if (forcePackPlayer == null) {
            plugin.log("Not checking " + player.name() + " because they are not in waiting.");
            return false;
        }

        boolean hasFailed = false;
        for (SpoofCheck check : forcePackPlayer.getChecks()) {
            final SpoofCheck.CheckStatus checkStatus = check.receiveStatus(status.name(), plugin::log);
            hasFailed = checkStatus == SpoofCheck.CheckStatus.FAILED;
            if (checkStatus == SpoofCheck.CheckStatus.CANCEL) {
                plugin.log("Cancelling status " + status + " as a check requested it.");
                event.setCancelled(true);
                return true;
            }
        }

        if (hasFailed) {
            plugin.log("Kicking player " + player.name() + " because they failed a check.");
            ensureMainThread(() -> player.kick(Component.translatable("forcepack.download_failed")));
        }

        return hasFailed;
    }

    private void callSpongeEvent(MultiVersionResourcePackStatusEvent event) {
        // Velocity doesn't correctly pass things to the backend server
        // Call sponge event manually for other plugins to handle status events

        // Can the ID be null? I'm not sure, let's just pass a random ID to avoid errors if this happens.
        // I doubt another plugin is using it anyway.
        Sponge.eventManager().post(new ResourcePackStatusEvent() {
            @Override
            public ServerSideConnection connection() {
                return event.getPlayer().connection();
            }

            @Override
            public GameProfile profile() {
                return event.getPlayer().profile();
            }

            @Override
            public Optional<ServerPlayer> player() {
                return Optional.of(event.getPlayer());
            }

            @Override
            public ResourcePackInfo pack() {
                return new ResourcePackInfo() {
                    @Override
                    public @NotNull UUID id() {
                        return event.getID() == null ? UUID.randomUUID() : event.getID();
                    }

                    @Override
                    public @NotNull URI uri() {
                        return URI.create("forcepack://proxy");
                    }

                    @Override
                    public @NotNull String hash() {
                        return "forcepack-proxy";
                    }
                };
            }

            @Override
            public ResourcePackStatus status() {
                return event.getStatus();
            }

            @Override
            public Cause cause() {
                return Cause.of(EventContext.empty(), plugin);
            }
        });
    }

    @Listener
    public void onPlayerJoin(ServerSideConnectionEvent.Join event) {
        ServerPlayer player = event.player();

        boolean geyser = getConfig().node("Server", "geyser").getBoolean() && GeyserUtil.isBedrockPlayer(player.uniqueId());
        boolean canBypass = player.hasPermission(Permissions.BYPASS) && getConfig().node("Server", "bypass-permission").getBoolean();
        plugin.log(player.name() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");

        if (canBypass || geyser) return;

        if (getConfig().node("velocity-mode").getBoolean()) {
            plugin.log("Velocity mode is enabled");
            plugin.addToWaiting(player.uniqueId(), Set.of());
            return;
        }

        final Set<ResourcePack> packs = plugin.getPacksForVersion(player);
        if (packs.isEmpty()) {
            plugin.log("Warning: Packs for player " + player.name() + " are empty.");
            return;
        }

        plugin.addToWaiting(player.uniqueId(), packs);

        for (ResourcePack pack : packs) {
            plugin.log("Sending pack " + pack.getUUID() + " to player " + player.name());
            final int version = ProtocolUtil.getProtocolVersion(player);
            final int maxSize = ClientVersion.getMaxSizeForVersion(version);
            final boolean forceSend = getConfig().node("Server", "force-invalid-size").getBoolean();
            if (!forceSend && pack.getSize() > maxSize) {
                if (plugin.debug()) plugin.getLogger().info("Not sending pack to {} because of excessive size for version {} ({}MB, {}MB).", player.name(), version, pack.getSize(), maxSize);
                continue;
            }

            plugin.getScheduler().executeOnMain(() -> this.runSetPackTask(player, pack, version));
        }
    }

    @Listener
    public void onReload(ForcePackReloadEvent event) {
        for (ServerPlayer onlinePlayer : Sponge.server().onlinePlayers()) {
            if (plugin.isWaiting(onlinePlayer)) continue;
            sentAccept.remove(onlinePlayer.uniqueId());
        }
    }

    private void runSetPackTask(ServerPlayer player, ResourcePack pack, int version) {
        AtomicReference<PlatformScheduler.ForcePackTask> task = new AtomicReference<>();
        Runnable packTask = () -> {
            if (plugin.isWaiting(player)) {
                plugin.log("Sent resource pack to player");
                pack.setResourcePack(player.uniqueId());
            }

            boolean sendTitle = plugin.getConfig().node("send-loading-title").getBoolean();
            if (sendTitle && sentAccept.containsKey(player.uniqueId())) {
                player.showTitle(Title.title(
                        Component.translatable("forcepack.download_start_title"),
                        Component.translatable("forcepack.download_start_subtitle"),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(30 * 50), Duration.ofMillis(10 * 50))));
            }

            final PlatformScheduler.ForcePackTask acquired = task.get();
            if (acquired != null && !plugin.isWaiting(player) && !sentAccept.containsKey(player.uniqueId())) {
                acquired.cancel();
            }
        };

        if (getConfig().node("Server", "Update GUI").getBoolean() && version <= 340) { // 340 is 1.12
            task.set(plugin.getScheduler().executeRepeating(packTask, 0L, getConfig().node("Server", "Update GUI Speed").getInt(20)));
        } else {
            packTask.run();
        }
    }

    @Listener
    public void onQuit(ServerSideConnectionEvent.Leave event) {
        ServerPlayer player = event.player();
        plugin.removeFromWaiting(player);
        sentAccept.remove(player.uniqueId());
    }

    private void ensureMainThread(Runnable runnable) {
        plugin.getScheduler().executeOnMain(runnable);
    }

    private ConfigurationNode getConfig() {
        return plugin.getConfig();
    }
}
