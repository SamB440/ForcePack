package net.islandearth.forcepack.spigot.utils;

import org.bukkit.Bukkit;

/**
 * @author SamB440
 */
public class Scheduler {
	
	private int task;

	public int getTask() {
		return task;
	}

	public void setTask(int task) {
		this.task = task;
	}

	public void cancel()
	{
		Bukkit.getScheduler().cancelTask(task);
	}
}