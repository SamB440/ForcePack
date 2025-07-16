package com.convallyria.forcepack.paper.listener;

import com.convallyria.forcepack.paper.ForcePackPaper;
import com.convallyria.forcepack.paper.event.MultiVersionResourcePackStatusEvent;
import net.kyori.adventure.resource.ResourcePackStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.UUID;

public class VelocityMessageListener implements PluginMessageListener {

    private static final String CHANNEL = "forcepack:status";

    private final ForcePackPaper plugin;

    public VelocityMessageListener(final ForcePackPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) {
            plugin.log("Ignoring channel " + channel);
            return;
        }

        final String data = new String(message);
        final String[] split = data.split(";");
        plugin.log("Posted event");

        final PlayerResourcePackStatusEvent.Status status = PlayerResourcePackStatusEvent.Status.valueOf(split[1]);
        plugin.getScheduler().executeAsync(() -> Bukkit.getPluginManager().callEvent(new MultiVersionResourcePackStatusEvent(player, UUID.fromString(split[0]), ResourcePackStatus.valueOf(status.name()), true, Boolean.parseBoolean(split[2]))));
    }
}
