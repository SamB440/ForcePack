package com.convallyria.forcepack.spigot.listener;

import com.convallyria.forcepack.spigot.ForcePackSpigot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.lang.reflect.Constructor;
import java.util.UUID;

public class VelocityMessageListener implements PluginMessageListener {

    private static final String CHANNEL = "forcepack:status";

    private final ForcePackSpigot plugin;

    public VelocityMessageListener(final ForcePackSpigot plugin) {
        this.plugin = plugin;
    }

    private static final Constructor<PlayerResourcePackStatusEvent> LEGACY_CONSTRUCTOR;

    static {
        try {
            LEGACY_CONSTRUCTOR = PlayerResourcePackStatusEvent.class.getConstructor(Player.class, PlayerResourcePackStatusEvent.Status.class);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(CHANNEL)) {
            plugin.log("Ignoring channel " + channel);
            return;
        }

        final String data = new String(message);
        plugin.log("Posted event");

        // Yeah this is cursed. But I don't know how else to handle statuses on the backend properly - velocity doesn't pass them correctly!
        //TODO can we make this better? Would be nice to handle new 1.20.3+ statuses on old servers being sent from velocity.
        // Perhaps using PacketEvents would be good to handle new statuses on old servers that aren't behind velocity as well.
        try {
            final PlayerResourcePackStatusEvent.Status status = PlayerResourcePackStatusEvent.Status.valueOf(data.split(";")[1]);
            Bukkit.getPluginManager().callEvent(new PlayerResourcePackStatusEvent(player, UUID.fromString(data.split(";")[0]), status));
        } catch (IllegalArgumentException ignored) {
            plugin.log("Unable to post status event because of mismatched versions");
            // Isn't present on this server version...
        } catch (NoSuchMethodError e) {
            // We are on a server version that doesn't have resource pack UUIDs
            try {
                Bukkit.getPluginManager().callEvent(LEGACY_CONSTRUCTOR.newInstance(player, PlayerResourcePackStatusEvent.Status.valueOf(data.split(";")[1])));
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
