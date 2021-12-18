package net.islandearth.forcepack.spigot.listener;

import net.islandearth.forcepack.spigot.ForcePack;
import net.islandearth.forcepack.spigot.resourcepack.ResourcePack;
import net.islandearth.forcepack.spigot.translation.Translations;
import net.islandearth.forcepack.spigot.utils.GeyserUtil;
import net.islandearth.forcepack.spigot.utils.Scheduler;
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

	private final Map<UUID, ResourcePack> waiting = new HashMap<>();
	private final ForcePack plugin;

	public ResourcePackListener(ForcePack plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onStatus(PlayerResourcePackStatusEvent prpse) {
		final Player player = prpse.getPlayer();
		boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player);
		if (!player.hasPermission("ForcePack.bypass") && !geyser) {
			waiting.remove(player.getUniqueId());
			plugin.log(player.getName() + " sent status: " + prpse.getStatus());

			final PlayerResourcePackStatusEvent.Status status = prpse.getStatus();

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
	public void onPlayerJoin(PlayerJoinEvent pje) {
		Player player = pje.getPlayer();
		boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player);
		if (!player.hasPermission("ForcePack.bypass") && !geyser) {
			final ResourcePack pack = plugin.getResourcePack();
			waiting.put(player.getUniqueId(), pack);
			Scheduler scheduler = new Scheduler();
			scheduler.setTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
				if (waiting.containsKey(player.getUniqueId())) {
					pack.setResourcePack(player);
				} else {
					scheduler.cancel();
				}
			}, 0L, getConfig().getInt("Server.Update GUI Speed", 20)));
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent pqe) {
		Player player = pqe.getPlayer();
		waiting.remove(player.getUniqueId());
	}

	private FileConfiguration getConfig() {
		return plugin.getConfig();
	}
}
