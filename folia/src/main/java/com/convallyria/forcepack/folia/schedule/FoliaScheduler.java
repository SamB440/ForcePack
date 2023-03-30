package com.convallyria.forcepack.folia.schedule;

import com.convallyria.forcepack.api.ForcePackAPI;
import com.convallyria.forcepack.api.schedule.PlatformScheduler;
import io.papermc.paper.threadedregions.RegionizedServerInitEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class FoliaScheduler extends PlatformScheduler<ForcePackAPI> implements Listener {

    public static final boolean RUNNING_FOLIA;

    static {
        boolean found;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServerInitEvent");
            found = true;
        } catch (final ReflectiveOperationException e) {
            found = false;
        }
        RUNNING_FOLIA = found;
    }

    private List<Runnable> initTasks = new ArrayList<>();

    public FoliaScheduler(ForcePackAPI api) {
        super(api);
        Bukkit.getPluginManager().registerEvents(this, (Plugin) api);
    }

    @EventHandler
    public void onServerInit(RegionizedServerInitEvent event) {
        for (final Runnable tasks : initTasks) {
            tasks.run();
        }
        initTasks = null;
    }

    @Override
    public void executeOnMain(Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().execute((Plugin) api, runnable);
    }

    @Override
    public ForcePackTask executeRepeating(Runnable runnable, long delay, long period) {
        final ScheduledTask task = Bukkit.getGlobalRegionScheduler().runAtFixedRate((Plugin) api, (s) -> runnable.run(), delay, period);
        return task::cancel;
    }

    @Override
    public void executeDelayed(Runnable runnable, long delay) {
        Bukkit.getGlobalRegionScheduler().runDelayed((Plugin) api, (s) -> runnable.run(), delay);
    }

    @Override
    public void registerInitTask(Runnable runnable) {
        if (initTasks == null) {
            throw new IllegalStateException("Server already initialised!");
        }
        initTasks.add(runnable);
    }
}
