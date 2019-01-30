package net.islandearth.forcepack.spigot.utils;

import org.bukkit.Bukkit;

import lombok.Getter;
import lombok.Setter;

/**
 * @author SamB440
 */
public class Scheduler {
	
	@Getter @Setter private int task;

	public void cancel()
	{
		Bukkit.getScheduler().cancelTask(task);
	}
}