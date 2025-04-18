package com.convallyria.forcepack.sponge.listener;

import com.convallyria.forcepack.sponge.ForcePackSponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;

public class ExemptionListener {

    private final ForcePackSponge plugin;

    public ExemptionListener(ForcePackSponge plugin) {
        this.plugin = plugin;
    }

    @Listener
    public void onDamage(DamageEntityEvent event) {
        if (!plugin.getConfig().node("prevent-damage").getBoolean()) return;

        event.cause().first(ServerPlayer.class).ifPresent(damager -> {
            if (damager.equals(event.entity())) return;
            if (plugin.isWaiting(damager)) {
                event.setCancelled(true);
                plugin.log("Cancelled damage for damager '" + damager.name() + "' due to resource pack not applied.");
            }
        });

        if (event.entity() instanceof ServerPlayer) {
            ServerPlayer damaged = (ServerPlayer) event.entity();
            if (plugin.isWaiting(damaged)) {
                event.setCancelled(true);
                plugin.log("Cancelled damage for player '" + damaged.name() + "' due to resource pack not applied.");
            }
        }
    }

    @Listener
    public void onMove(MoveEntityEvent event) {
        if (!(event.entity() instanceof ServerPlayer)) return;
        if (!plugin.getConfig().node("prevent-movement").getBoolean()) return;

        ServerPlayer player = (ServerPlayer) event.entity();
        if (plugin.isWaiting(player)) {
            event.setCancelled(true);
            plugin.log("Cancelled movement for player '" + player.name() + "' due to resource pack not applied.");
        }
    }
}
