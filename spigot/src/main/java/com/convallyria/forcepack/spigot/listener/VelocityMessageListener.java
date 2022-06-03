package com.convallyria.forcepack.spigot.listener;

import com.convallyria.forcepack.spigot.ForcePackSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class VelocityMessageListener implements PluginMessageListener {

    private static String CHANNEL = "forcepack:status";

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
        if (!data.equals("SUCCESSFUL")) {
            plugin.log("Data was not 'successful'. Ignoring.");
            return;
        }

        plugin.log("Posted event");
        Bukkit.getPluginManager().callEvent(new PlayerResourcePackStatusEvent(player, PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED));
    }
}
