package com.convallyria.forcepack.spigot.listener;

import com.convallyria.forcepack.spigot.ForcePackSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;

public class VelocityMessageListener implements PluginMessageListener {

    private static final String CHANNEL = "forcepack:status";

    private final ForcePackSpigot plugin;

    public VelocityMessageListener(final ForcePackSpigot plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) {
            plugin.log("Ignoring channel " + channel);
            return;
        }

        final String data = new String(message);
        plugin.log("Posted event");
        Bukkit.getPluginManager().callEvent(new PlayerResourcePackStatusEvent(player, UUID.fromString(data.split(";")[0]), PlayerResourcePackStatusEvent.Status.valueOf(data.split(";")[1])));
    }
}
