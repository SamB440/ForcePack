package com.convallyria.forcepack.spigot.schedule;

import com.convallyria.forcepack.api.schedule.PlatformScheduler;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class BukkitScheduler extends PlatformScheduler<ForcePackSpigot> {

    public BukkitScheduler(ForcePackSpigot api) {
        super(api);
    }

    @Override
    public void executeOnMain(Runnable runnable) {
        Bukkit.getScheduler().runTask(api, runnable);
    }

    @Override
    public ForcePackTask executeRepeating(Runnable runnable, long delay, long period) {
        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(api, runnable, delay, period);
        return task::cancel;
    }

    @Override
    public void executeDelayed(Runnable runnable, long delay) {
        Bukkit.getScheduler().runTaskLater(api, runnable, delay);
    }

    @Override
    public void registerInitTask(Runnable runnable) {
        executeOnMain(runnable);
    }
}
