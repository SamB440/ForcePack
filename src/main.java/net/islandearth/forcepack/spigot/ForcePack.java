package net.islandearth.forcepack.spigot;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import net.islandearth.forcepack.spigot.listener.ResourcePackListener;

public class ForcePack extends JavaPlugin implements Listener {
	
	private Logger log = Bukkit.getLogger();
	
	public void onEnable() {
		createConfig();
		registerListeners();
		log.info("[ForcePack] Enabled!");
	}
	
	public void onDisable() {
		log.info("[ForcePack] Disabled!");
	}
	
	private void registerListeners() {
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new ResourcePackListener(this), this);
	}
	
	public void createConfig() {
		getConfig().options().copyDefaults(true);
		getConfig().addDefault("Server.Messages.Declined_Message", "&cYou must accept the resource pack to play on our server. Don't know how? Check out &ehttp://www.islandearth.net/rp.html.");
		getConfig().addDefault("Server.Messages.Accepted_Message", "&aThank you for accepting our resource pack! You can now play.");
		getConfig().addDefault("Server.Messages.Failed_Download_Message", "&cThe resource pack download failed. Please reconnect and try again.");
		getConfig().addDefault("Server.Actions.On_Accept.Command", "say [player] accepted the resource pack!");
		getConfig().addDefault("Server.Actions.On_Deny.Command", "say [player] denied the resource pack!");
		getConfig().addDefault("Server.Actions.On_Fail.Command", "say [player] failed to download the resource pack!");
		getConfig().addDefault("Server.Timeout_ticks", 550);
		getConfig().addDefault("Server.kick", true);
		saveConfig();
	}
}
