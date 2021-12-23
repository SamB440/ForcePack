package com.convallyria.forcepack.spigot.listener;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.translation.Translations;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.spigot.utils.Scheduler;
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
	private final ForcePackSpigot plugin;

	public ResourcePackListener(ForcePackSpigot plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onStatus(PlayerResourcePackStatusEvent event) {
		final Player player = event.getPlayer();
		boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
		if (!player.hasPermission("ForcePack.bypass") && !geyser) {
			waiting.remove(player.getUniqueId());
			plugin.log(player.getName() + " sent status: " + event.getStatus());

			final PlayerResourcePackStatusEvent.Status status = event.getStatus();

			for (String cmd : getConfig().getStringList("Server.Actions." + status.name() + ".Commands")) {
				Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("[player]", player.getName()));
			}

			final boolean kick = getConfig().getBoolean("Server.Actions." + status.name() + ".kick");

			switch (status) {
				case DECLINED -> {
					if (kick) player.kickPlayer(Translations.DECLINED.get(player));
					else Translations.DECLINED.send(player);
				}
				case FAILED_DOWNLOAD -> {
					if (kick) player.kickPlayer(Translations.DOWNLOAD_FAILED.get(player));
					else Translations.DOWNLOAD_FAILED.send(player);
				}
				case SUCCESSFULLY_LOADED -> {
					if (kick) player.kickPlayer(Translations.ACCEPTED.get(player));
					else Translations.ACCEPTED.send(player);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
		if (!player.hasPermission("ForcePack.bypass") && !geyser) {
			final ResourcePack pack = plugin.getResourcePacks().get(0);
			waiting.put(player.getUniqueId(), pack);
			Scheduler scheduler = new Scheduler();
			scheduler.setTask(Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
				if (waiting.containsKey(player.getUniqueId())) {
					pack.setResourcePack(player.getUniqueId());
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
