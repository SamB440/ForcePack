package com.convallyria.forcepack.spigot.listener;

import com.convallyria.forcepack.spigot.ForcePackSpigot;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class ExemptionListener implements Listener {

    private final ForcePackSpigot plugin;

    public ExemptionListener(ForcePackSpigot plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("prevent-damage")) return;

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (plugin.isWaiting(player)) {
                event.setCancelled(true);
                plugin.log("Cancelled damage for player '" + player.getName() + "' due to resource pack not applied.");
            }
        }

        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            if (plugin.isWaiting(damager)) {
                event.setCancelled(true);
                plugin.log("Cancelled damage for damager '" + damager.getName() + "' due to resource pack not applied.");
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfig().getBoolean("prevent-damage")) return;

        if (event.getEntity() instanceof Player) {
            Player damaged = (Player) event.getEntity();
            if (plugin.isWaiting(damaged)) {
                event.setCancelled(true);
                plugin.log("Cancelled damage for player '" + damaged.getName() + "' due to resource pack not applied.");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("prevent-movement")) return;

        final Player player = event.getPlayer();
        if (plugin.isWaiting(player)) {
            event.setCancelled(true);
            plugin.log("Cancelled movement for player '" + player.getName() + "' due to resource pack not applied.");
        }
    }
}
