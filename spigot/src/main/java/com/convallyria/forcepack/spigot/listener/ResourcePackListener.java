package com.convallyria.forcepack.spigot.listener;

import com.convallyria.forcepack.api.check.SpoofCheck;
import com.convallyria.forcepack.api.permission.Permissions;
import com.convallyria.forcepack.api.player.ForcePackPlayer;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.schedule.PlatformScheduler;
import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.event.ForcePackReloadEvent;
import com.convallyria.forcepack.spigot.event.MultiVersionResourcePackStatusEvent;
import com.convallyria.forcepack.spigot.translation.Translations;
import com.convallyria.forcepack.spigot.util.ProtocolUtil;
import net.kyori.adventure.resource.ResourcePackStatus;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class ResourcePackListener implements Listener {

    private final ForcePackSpigot plugin;

    private final Map<UUID, Long> sentAccept = new ConcurrentHashMap<>();

    public ResourcePackListener(final ForcePackSpigot plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStatus(MultiVersionResourcePackStatusEvent event) {
        final Player player = event.getPlayer();
        final UUID id = event.getID();

        boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        boolean canBypass = player.hasPermission(Permissions.BYPASS) && getConfig().getBoolean("Server.bypass-permission");
        plugin.log(player.getName() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");

        if (canBypass || geyser) {
            return;
        }

        final ResourcePackStatus status = event.getStatus();
        plugin.log(player.getName() + " sent status: " + status);

        if (!plugin.velocityMode && tryValidateHacks(player, event, status)) return;

        // If we did not set this resource pack, ignore
        if (!event.isProxy() && !plugin.isWaitingFor(player, id)) {
            plugin.log("Ignoring resource pack " + id + " because it wasn't set by ForcePack.");
            return;
        } else if (event.isProxy()) {
            plugin.log("Resource pack with id " + id + " sent by proxy.");
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

        for (String cmd : getConfig().getStringList("Server.Actions." + status.name() + ".Commands")) {
            ensureMainThread(() -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("[player]", player.getName())));
        }

        // Don't execute kicks - handled by proxy
        if (plugin.velocityMode) {
            plugin.getScheduler().executeOnMain(() -> this.callBukkitEvent(event));
            return;
        }

        final boolean kick = getConfig().getBoolean("Server.Actions." + status.name() + ".kick");

        switch (status) {
            case ACCEPTED: {
                sentAccept.put(player.getUniqueId(), System.currentTimeMillis());
                break;
            }
            case DECLINED: {
                ensureMainThread(() -> {
                    if (kick) player.kickPlayer(Translations.DECLINED.get(player));
                    else Translations.DECLINED.send(player);
                });

                sentAccept.remove(player.getUniqueId());
                break;
            }
            case DISCARDED:
            case INVALID_URL:
            case FAILED_RELOAD:
            case FAILED_DOWNLOAD: {
                ensureMainThread(() -> {
                    if (kick) player.kickPlayer(Translations.DOWNLOAD_FAILED.get(player));
                    else Translations.DOWNLOAD_FAILED.send(player);
                });

                sentAccept.remove(player.getUniqueId());
                break;
            }
            case SUCCESSFULLY_LOADED: {
                if (kick) ensureMainThread(() -> player.kickPlayer(Translations.ACCEPTED.get(player)));
                else {
                    ensureMainThread(() -> Translations.ACCEPTED.send(player));
                    boolean sendTitle = plugin.getConfig().getBoolean("send-loading-title");
                    if (sendTitle) player.sendTitle(null, null, 0, 0, 0); // resetTitle doesn't clear subtitle
                }
                break;
            }
        }
    }

    private boolean tryValidateHacks(Player player, MultiVersionResourcePackStatusEvent event, ResourcePackStatus status) {
        final boolean tryPrevent = getConfig().getBoolean("try-to-stop-fake-accept-hacks", true);
        if (!tryPrevent) return false;

        final ForcePackPlayer forcePackPlayer = plugin.getForcePackPlayer(player).orElse(null);
        if (forcePackPlayer == null) {
            plugin.log("Not checking " + player.getName() + " because they are not in waiting.");
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
            plugin.log("Kicking player " + player.getName() + " because they failed a check.");
            ensureMainThread(() -> player.kickPlayer(Translations.DOWNLOAD_FAILED.get(player)));
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

    private void callBukkitEvent(MultiVersionResourcePackStatusEvent event) {
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
            bukkit = new PlayerResourcePackStatusEvent(event.getPlayer(), event.getID() == null ? UUID.randomUUID() : event.getID(), bukkitStatus);
        } catch (NoSuchMethodError ignored) {
            // We are on a server version that doesn't have resource pack UUIDs.
            try {
                bukkit = LEGACY_CONSTRUCTOR.newInstance(event.getPlayer(), bukkitStatus);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        Bukkit.getPluginManager().callEvent(bukkit);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        boolean canBypass = player.hasPermission(Permissions.BYPASS) && getConfig().getBoolean("Server.bypass-permission");
        plugin.log(player.getName() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");

        if (canBypass || geyser) return;

        if (plugin.velocityMode) {
            plugin.log("Velocity mode is enabled");
            plugin.addToWaiting(player.getUniqueId(), Set.of());
            return;
        }

        final Set<ResourcePack> packs = plugin.getPacksForVersion(player);
        if (packs.isEmpty()) {
            plugin.log("Warning: Packs for player " + player.getName() + " are empty.");
            return;
        }

        plugin.addToWaiting(player.getUniqueId(), packs);

        for (ResourcePack pack : packs) {
            plugin.log("Sending pack " + pack.getUUID() + " to player " + player.getName());
            final int version = ProtocolUtil.getProtocolVersion(player);
            final int maxSize = ClientVersion.getMaxSizeForVersion(version);
            final boolean forceSend = getConfig().getBoolean("Server.force-invalid-size");
            if (!forceSend && pack.getSize() > maxSize) {
                if (plugin.debug()) plugin.getLogger().info(String.format("Not sending pack to %s because of excessive size for version %d (%dMB, %dMB).", player.getName(), version, pack.getSize(), maxSize));
                return;
            }

            plugin.getScheduler().executeOnMain(() -> this.runSetPackTask(player, pack, version));
        }
    }

    @EventHandler
    public void onReload(ForcePackReloadEvent event) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (plugin.isWaiting(onlinePlayer)) continue;
            sentAccept.remove(onlinePlayer.getUniqueId());
        }
    }

    private void runSetPackTask(Player player, ResourcePack pack, int version) {
        AtomicReference<PlatformScheduler.ForcePackTask> task = new AtomicReference<>();
        Runnable packTask = () -> {
            if (plugin.isWaiting(player)) {
                plugin.log("Sent resource pack to player");
                pack.setResourcePack(player.getUniqueId());
            }

            boolean sendTitle = plugin.getConfig().getBoolean("send-loading-title");
            if (sendTitle && sentAccept.containsKey(player.getUniqueId())) {
                player.sendTitle(Translations.DOWNLOAD_START_TITLE.get(player), Translations.DOWNLOAD_START_SUBTITLE.get(player), 0, 30, 10);
            }

            final PlatformScheduler.ForcePackTask acquired = task.get();
            if (acquired != null && !plugin.isWaiting(player) && !sentAccept.containsKey(player.getUniqueId())) {
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
        plugin.removeFromWaiting(player);
        sentAccept.remove(player.getUniqueId());
    }

    private void ensureMainThread(Runnable runnable) {
        plugin.getScheduler().executeOnMain(runnable);
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
