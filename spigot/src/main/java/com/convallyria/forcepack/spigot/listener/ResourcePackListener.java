package com.convallyria.forcepack.spigot.listener;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.translation.Translations;
import com.convallyria.forcepack.spigot.utils.Scheduler;
import com.viaversion.viaversion.api.Via;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ResourcePackListener implements Listener {

    private final ForcePackSpigot plugin;

    private final Map<UUID, Long> sentAccept = new HashMap<>();

    public ResourcePackListener(final ForcePackSpigot plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        final long now = System.currentTimeMillis();
        final Player player = event.getPlayer();
        boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        boolean canBypass = player.hasPermission("ForcePack.bypass") && getConfig().getBoolean("Server.bypass-permission");
        plugin.log(player.getName() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");

        if (!canBypass && !geyser) {
            plugin.getWaiting().remove(player.getUniqueId());
            plugin.log(player.getName() + " sent status: " + event.getStatus());

            final PlayerResourcePackStatusEvent.Status status = event.getStatus();

            for (String cmd : getConfig().getStringList("Server.Actions." + status.name() + ".Commands")) {
                ensureMainThread(() -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("[player]", player.getName())));
            }

            // Don't execute kicks - handled by proxy
            if (plugin.velocityMode) return;

            final boolean kick = getConfig().getBoolean("Server.Actions." + status.name() + ".kick");

            final boolean tryPrevent = getConfig().getBoolean("try-to-stop-fake-accept-hacks", true);
            if (tryPrevent) {
                if (status == PlayerResourcePackStatusEvent.Status.ACCEPTED) {
                    if (sentAccept.containsKey(player.getUniqueId())) {
                        plugin.log("Kicked player " + player.getName() + " because they are sending fake resource pack statuses (accepted sent twice).");
                        ensureMainThread(() -> player.kickPlayer(Translations.DECLINED.get(player)));
                        return;
                    }
                    sentAccept.put(player.getUniqueId(), now);
                } else if (status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
                    if (!sentAccept.containsKey(player.getUniqueId())) {
                        plugin.log("Kicked player " + player.getName() + " because they are sending fake resource pack statuses (order not maintained).");
                        ensureMainThread(() -> player.kickPlayer(Translations.DOWNLOAD_FAILED.get(player)));
                        return;
                    }

                    long time = now - sentAccept.remove(player.getUniqueId());
                    if (time <= 10) {
                        plugin.log("Kicked player " + player.getName() + " because they are sending fake resource pack statuses (sent too fast).");
                        ensureMainThread(() -> player.kickPlayer(Translations.DOWNLOAD_FAILED.get(player)));
                        return;
                    }
                }
            }

            switch (status) {
                case DECLINED: {
                    if (kick) ensureMainThread(() -> player.kickPlayer(Translations.DECLINED.get(player)));
                    else Translations.DECLINED.send(player);
                    break;
                }
                case FAILED_DOWNLOAD: {
                    if (kick) ensureMainThread(() -> player.kickPlayer(Translations.DOWNLOAD_FAILED.get(player)));
                    else Translations.DOWNLOAD_FAILED.send(player);
                    break;
                }
                case SUCCESSFULLY_LOADED: {
                    if (kick) ensureMainThread(() -> player.kickPlayer(Translations.ACCEPTED.get(player)));
                    else Translations.ACCEPTED.send(player);
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        boolean canBypass = player.hasPermission("ForcePack.bypass") && getConfig().getBoolean("Server.bypass-permission");
        plugin.log(player.getName() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");

        if (!canBypass && !geyser) {
            if (plugin.velocityMode) {
                plugin.log("Velocity mode is enabled");
                plugin.getWaiting().put(player.getUniqueId(), null);
                return;
            }

            final ResourcePack pack = plugin.getResourcePacks().get(0);
            plugin.getWaiting().put(player.getUniqueId(), pack);
            Scheduler scheduler = new Scheduler();
            Runnable packTask = () -> {
                if (plugin.getWaiting().containsKey(player.getUniqueId())) {
                    plugin.log("Sent resource pack to player");
                    pack.setResourcePack(player.getUniqueId());
                } else {
                    scheduler.cancel();
                }
            };

            final boolean viaversion = Bukkit.getPluginManager().isPluginEnabled("ViaVersion");
            final int version = viaversion ? Via.getAPI().getPlayerVersion(player) : 393; // 393 is 1.13 - default to this
            final int maxSize = ClientVersion.getMaxSizeForVersion(version);
            final boolean forceSend = getConfig().getBoolean("Server.force-invalid-size");
            if (!forceSend && pack.getSize() > maxSize) {
                if (plugin.debug()) plugin.getLogger().info(String.format("Not sending pack to %s because of excessive size for version %d (%dMB, %dMB).", player.getName(), version, pack.getSize(), maxSize));
                return;
            }

            if (getConfig().getBoolean("Server.Update GUI") && version <= 340) { // 340 is 1.12
                scheduler.setTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, packTask,
                        0L, getConfig().getInt("Server.Update GUI Speed", 20)));
            } else {
                packTask.run();
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent pqe) {
        Player player = pqe.getPlayer();
        plugin.getWaiting().remove(player.getUniqueId());
    }

    private void ensureMainThread(Runnable runnable) {
        if (!Bukkit.isPrimaryThread()) Bukkit.getScheduler().runTask(plugin, runnable);
        else runnable.run();
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
