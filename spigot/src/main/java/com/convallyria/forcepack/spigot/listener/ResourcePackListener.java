package com.convallyria.forcepack.spigot.listener;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
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

public class ResourcePackListener implements Listener {

    private final ForcePackSpigot plugin;

    public ResourcePackListener(final ForcePackSpigot plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        final Player player = event.getPlayer();
        boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        boolean canBypass = player.hasPermission("ForcePack.bypass") && getConfig().getBoolean("Server.bypass-permission");
        if (!canBypass && !geyser) {
            plugin.getWaiting().remove(player.getUniqueId());
            plugin.log(player.getName() + " sent status: " + event.getStatus());

            final PlayerResourcePackStatusEvent.Status status = event.getStatus();

            for (String cmd : getConfig().getStringList("Server.Actions." + status.name() + ".Commands")) {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("[player]", player.getName()));
            }

            final boolean kick = getConfig().getBoolean("Server.Actions." + status.name() + ".kick");

            switch (status) {
                case DECLINED: {
                    if (kick) player.kickPlayer(Translations.DECLINED.get(player));
                    else Translations.DECLINED.send(player);
                    break;
                }
                case FAILED_DOWNLOAD: {
                    if (kick) player.kickPlayer(Translations.DOWNLOAD_FAILED.get(player));
                    else Translations.DOWNLOAD_FAILED.send(player);
                    break;
                }
                case SUCCESSFULLY_LOADED: {
                    if (kick) player.kickPlayer(Translations.ACCEPTED.get(player));
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
        if (!canBypass && !geyser) {
            final ResourcePack pack = plugin.getResourcePacks().get(0);
            plugin.getWaiting().put(player.getUniqueId(), pack);
            Scheduler scheduler = new Scheduler();
            Runnable packTask = () -> {
                if (plugin.getWaiting().containsKey(player.getUniqueId())) {
                    pack.setResourcePack(player.getUniqueId());
                } else {
                    scheduler.cancel();
                }
            };

            final boolean viaversion = Bukkit.getPluginManager().isPluginEnabled("ViaVersion");
            final int version = viaversion ? Via.getAPI().getPlayerVersion(player) : 393; // 393 is 1.13 - default to this
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

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
