package com.convallyria.forcepack.paper.listener;

import com.convallyria.forcepack.api.check.SpoofCheck;
import com.convallyria.forcepack.api.permission.Permissions;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.schedule.PlatformScheduler;
import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.paper.ForcePackPaper;
import com.convallyria.forcepack.paper.event.ForcePackReloadEvent;
import com.convallyria.forcepack.paper.event.MultiVersionResourcePackStatusEvent;
import com.convallyria.forcepack.paper.player.ForcePackPaperPlayer;
import com.convallyria.forcepack.paper.translation.Translations;
import com.convallyria.forcepack.paper.util.GameProfile;
import com.convallyria.forcepack.paper.util.ProtocolUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class ResourcePackListener implements Listener {

    private final ForcePackPaper plugin;

    private final Map<UUID, Long> sentAccept = new ConcurrentHashMap<>();

    public ResourcePackListener(final ForcePackPaper plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStatus(MultiVersionResourcePackStatusEvent event) {
        final GameProfile profile = event.getProfile();
        final UUID uuid = profile.uniqueId();

        final ForcePackPaperPlayer forcePackPlayer = plugin.getForcePackPlayer(uuid).orElse(null);
        if (forcePackPlayer == null) {
            // Player isn't valid - wasn't added to waiting (exempt or not yet tracked)
            return;
        }

        final UUID packId = event.getID();

        if (plugin.temporaryExemptedPlayers.remove(uuid)) {
            plugin.log("Ignoring player " + uuid + " as they have a one-off exemption.");
            return;
        }

        final ResourcePackStatus status = event.getStatus();
        plugin.log(uuid + " sent status: " + status);

        if (!plugin.velocityMode && tryValidateHacks(profile, event, status)) return;

        // If we did not set this resource pack, ignore
        if (!event.isProxy() && !plugin.isWaitingFor(uuid, packId)) {
            plugin.log("Ignoring resource pack " + packId + " because it wasn't set by ForcePack.");
            return;
        } else if (event.isProxy()) {
            plugin.log("Resource pack with id " + packId + " sent by proxy. Removal state: " + event.isProxyRemove());
        }

        // Only remove from waiting if they actually loaded the resource pack, rather than any status
        // Declined/failed is valid and should be allowed, server owner decides whether they get kicked
        if (status != ResourcePackStatus.ACCEPTED && status != ResourcePackStatus.DOWNLOADED) {
            if (event.isProxy()) {
                if (event.isProxyRemove()) {
                    plugin.removeFromWaiting(uuid);
                }
            } else {
                plugin.processWaitingResourcePack(uuid, packId);
            }
        }

        for (String cmd : getConfig().getStringList("Server.Actions." + status.name() + ".Commands")) {
            ensureMainThread(() -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("[player]", profile.name())));
        }

        // Don't execute kicks - handled by proxy
        if (plugin.velocityMode) {
            plugin.getScheduler().executeOnMain(() -> this.callBukkitEvent(profile, event));
            return;
        }

        final boolean kick = getConfig().getBoolean("Server.Actions." + status.name() + ".kick");

        switch (status) {
            case ACCEPTED: {
                sentAccept.put(uuid, System.currentTimeMillis());
                break;
            }
            case DECLINED: {
                if (kick) {
                    forcePackPlayer.closeConnection(Component.join(JoinConfiguration.newlines(), Translations.DECLINED.get()));
                } else {
                    ensureMainThread(() -> forcePackPlayer.player().ifPresent(Translations.DECLINED::send));
                }

                sentAccept.remove(uuid);
                break;
            }
            case DISCARDED:
            case INVALID_URL:
            case FAILED_RELOAD:
            case FAILED_DOWNLOAD: {
                if (kick) {
                    forcePackPlayer.closeConnection(Component.join(JoinConfiguration.newlines(), Translations.DOWNLOAD_FAILED.get()));
                } else {
                    ensureMainThread(() -> forcePackPlayer.player().ifPresent(Translations.DOWNLOAD_FAILED::send));
                }

                sentAccept.remove(uuid);
                break;
            }
            case SUCCESSFULLY_LOADED: {
                if (kick) {
                    forcePackPlayer.closeConnection(Component.join(JoinConfiguration.newlines(), Translations.ACCEPTED.get()));
                } else {
                    ensureMainThread(() -> forcePackPlayer.player().ifPresent(player -> {
                        Translations.ACCEPTED.send(player);
                        boolean sendTitle = plugin.getConfig().getBoolean("send-loading-title");
                        if (sendTitle) player.sendTitle(null, null, 0, 0, 0); // resetTitle doesn't clear subtitle
                    }));
                }
                break;
            }
        }
    }

    private boolean tryValidateHacks(GameProfile profile, MultiVersionResourcePackStatusEvent event, ResourcePackStatus status) {
        final boolean tryPrevent = getConfig().getBoolean("try-to-stop-fake-accept-hacks", true);
        if (!tryPrevent) return false;

        final ForcePackPaperPlayer forcePackPlayer = plugin.getForcePackPlayer(profile.uniqueId()).orElse(null);
        if (forcePackPlayer == null) {
            plugin.log("Not checking " + profile.name() + " because they are not in waiting.");
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
            plugin.log("Kicking player " + profile.name() + " because they failed a check.");
            forcePackPlayer.closeConnection(Component.empty());
        }

        return hasFailed;
    }

    private static final Constructor<PlayerResourcePackStatusEvent> LEGACY_CONSTRUCTOR;

    static {
        Constructor<PlayerResourcePackStatusEvent> constructor;
        try {
            constructor = PlayerResourcePackStatusEvent.class.getConstructor(Player.class, PlayerResourcePackStatusEvent.Status.class);
        } catch (NoSuchMethodException ignored) {
            // We are on a server version that uses resource pack UUIDs.
            constructor = null;
        }

        LEGACY_CONSTRUCTOR = constructor;
    }

    private void callBukkitEvent(GameProfile profile, MultiVersionResourcePackStatusEvent event) {
        final Player player = Bukkit.getPlayer(profile.uniqueId());
        if (player == null) {
            // Paper doesn't support configuration phase resource pack events. Nice.
            return;
        }

        // Velocity doesn't correctly pass things to the backend server
        // Call bukkit event manually for other plugins to handle status events
        PlayerResourcePackStatusEvent.Status bukkitStatus;
        try {
            bukkitStatus = PlayerResourcePackStatusEvent.Status.valueOf(event.getStatus().name());
        } catch (IllegalArgumentException ignored) {
            // The server version we are on doesn't support this status
            return;
        }

        PlayerResourcePackStatusEvent bukkit;
        try {
            // Can the ID be null? I'm not sure, let's just pass a random ID to avoid errors if this happens.
            // I doubt another plugin is using it anyway.
            bukkit = new PlayerResourcePackStatusEvent(player, event.getID() == null ? UUID.randomUUID() : event.getID(), bukkitStatus);
        } catch (NoSuchMethodError ignored) {
            // We are on a server version that doesn't have resource pack UUIDs.
            try {
                bukkit = LEGACY_CONSTRUCTOR.newInstance(player, bukkitStatus);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        Bukkit.getPluginManager().callEvent(bukkit);
    }

    private static final boolean HAS_CONFIGURATION_PHASE = PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_20_2);

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasMetadata("NPC") || player.hasMetadata("fake-player")) return;

        final UUID uuid = player.getUniqueId();
        boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(uuid);
        boolean canBypass = player.hasPermission(Permissions.BYPASS)
                && getConfig().getBoolean("Server.bypass-permission")
                && !plugin.velocityMode;
        plugin.log(player.getName() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");

        if (canBypass || geyser) {
            if (HAS_CONFIGURATION_PHASE) {
                // Player was added to waiting in configuration start phase, but we can now check their permissions and they shouldn't be waiting
                plugin.removeFromWaiting(uuid);
            }
            return;
        }

        if (plugin.velocityMode) {
            plugin.log("Velocity mode is enabled");
            // Player was already added in configuration start phase, don't add again as we might have already handled resource packs
            if (!HAS_CONFIGURATION_PHASE) {
                plugin.addToWaiting(uuid, Set.of());
            }
            return;
        }

        final Set<ResourcePack> packs = plugin.getPacksForVersion(uuid);
        if (packs.isEmpty()) {
            plugin.log("Warning: Packs for player " + player.getName() + " are empty.");
            return;
        }

        plugin.addToWaiting(uuid, packs);

        for (ResourcePack pack : packs) {
            plugin.log("Sending pack " + pack.getUUID() + " to player " + player.getName());
            final int version = ProtocolUtil.getProtocolVersion(uuid);
            final int maxSize = ClientVersion.getMaxSizeForVersion(version);
            final boolean forceSend = getConfig().getBoolean("Server.force-invalid-size");
            if (!forceSend && pack.getSize() > maxSize) {
                if (plugin.debug()) plugin.getLogger().info(String.format("Not sending pack to %s because of excessive size for version %d (%dMB, %dMB).", player.getName(), version, pack.getSize(), maxSize));
                continue;
            }

            plugin.getScheduler().executeOnMain(() -> this.runSetPackTask(player, pack, version));
        }
    }

    @EventHandler
    public void onReload(ForcePackReloadEvent event) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (plugin.isWaiting(onlinePlayer.getUniqueId())) continue;
            sentAccept.remove(onlinePlayer.getUniqueId());
        }
    }

    private void runSetPackTask(Player player, ResourcePack pack, int version) {
        final UUID uuid = player.getUniqueId();
        AtomicReference<PlatformScheduler.ForcePackTask> task = new AtomicReference<>();
        Runnable packTask = () -> {
            if (plugin.isWaiting(uuid)) {
                plugin.log("Sent resource pack to player");
                pack.setResourcePack(uuid);
            }

            boolean sendTitle = plugin.getConfig().getBoolean("send-loading-title");
            if (sendTitle && sentAccept.containsKey(uuid)) {
                plugin.adventure().player(player).showTitle(Title.title(Translations.DOWNLOAD_START_TITLE.get().get(0),
                        Translations.DOWNLOAD_START_SUBTITLE.get().get(0), 0, 30, 10));
            }

            final PlatformScheduler.ForcePackTask acquired = task.get();
            if (acquired != null && !plugin.isWaiting(uuid) && !sentAccept.containsKey(uuid)) {
                acquired.cancel();
            }
        };

        if (getConfig().getBoolean("Server.Update GUI") && version <= 340) { // 340 is 1.12
            task.set(plugin.getScheduler().executeRepeating(packTask, 0L, getConfig().getInt("Server.Update GUI Speed", 20)));
        } else {
            packTask.run();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent pqe) {
        Player player = pqe.getPlayer();
        plugin.removeFromWaiting(player.getUniqueId());
        sentAccept.remove(player.getUniqueId());
        plugin.temporaryExemptedPlayers.remove(player.getUniqueId());
    }

    private void ensureMainThread(Runnable runnable) {
        plugin.getScheduler().executeOnMain(runnable);
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
