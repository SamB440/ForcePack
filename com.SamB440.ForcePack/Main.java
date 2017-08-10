package com.SamB440.ForcePack;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Logger;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
	ArrayList<UUID> waiting = new ArrayList<UUID>();
	String cpx = "[ForcePack]";
	public final Logger log = Logger.getLogger("Minecraft");
	
	public void onEnable()
	{
		Bukkit.getServer().getPluginManager().registerEvents(this, this);
		getConfig().options().copyDefaults(true);
		createConfig();
		log.info(cpx + "Enabled!");
	}
	public void onDisable()
	{
		log.info(cpx + "Disabled!");
	}
	@EventHandler
	public void ResourcePackStatus(PlayerResourcePackStatusEvent prpse)
	{
		final Player p = prpse.getPlayer();
		if(prpse.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED)
		{
			log.info(p.getName() + " declined the resource pack.");
			waiting.remove(p.getUniqueId());
			p.kickPlayer(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Server.Messages.Declined_Message")));
		}
		else if(prpse.getStatus() == PlayerResourcePackStatusEvent.Status.ACCEPTED || prpse.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED)
		{
			log.info(p.getName() + " accepted the resource pack.");
			waiting.remove(p.getUniqueId());
			p.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Server.Messages.Accepted_Message")));
		}
		else if(prpse.getStatus() == PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD)
		{
		waiting.remove(p.getUniqueId());
		p.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Server.Messages.Failed_Download_Message")));
		}
	}	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent pje)
	{
		final Player p = pje.getPlayer();
		waiting.add(p.getUniqueId());
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable()
		{
			@Override
			public void run() 
			{
				if(waiting.contains(p.getUniqueId()))
				{
					p.kickPlayer(ChatColor.translateAlternateColorCodes('&', getConfig().getString("Server.Messages.Declined_Message")));
				}
			}
		}, getConfig().getInt("Server.Timeout_ticks"));
	}
	public void createConfig()
	{
		getConfig().addDefault("Server.Messages.Declined_Message", "&cYou must accept the resource pack to play on our server. Don't know how? Check out &ehttp://s.moep.tv/rp.");
		getConfig().addDefault("Server.Messages.Accepted_Message", "&aThank you for accepting our resource pack! You can now play.");
		getConfig().addDefault("Server.Messages.Failed_Download_Message", "&cThe resource pack download failed. Please reconnect and try again.");
		getConfig().addDefault("Server.Timeout_ticks", 550);
		saveConfig();
	}
}
