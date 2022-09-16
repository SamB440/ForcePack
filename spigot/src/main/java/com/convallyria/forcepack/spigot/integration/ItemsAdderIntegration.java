package com.convallyria.forcepack.spigot.integration;

import com.convallyria.forcepack.spigot.ForcePackSpigot;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ItemsAdderIntegration implements Listener {

    private final ForcePackSpigot plugin;
    private final Runnable run;

    public ItemsAdderIntegration(ForcePackSpigot plugin, Runnable run) {
        this.plugin = plugin;
        this.run = run;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onLoad(ItemsAdderLoadDataEvent event) {
        plugin.getLogger().info("Hooked into Items Adder data load");
        run.run();
    }
}
