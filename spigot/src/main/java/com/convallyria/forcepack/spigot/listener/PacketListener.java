package com.convallyria.forcepack.spigot.listener;

import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.event.MultiVersionResourcePackStatusEvent;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientResourcePackStatus;
import net.kyori.adventure.resource.ResourcePackStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.UUID;

public class PacketListener extends PacketListenerAbstract {

    private final ForcePackSpigot plugin;

    public PacketListener(ForcePackSpigot plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.RESOURCE_PACK_STATUS) {
            WrapperPlayClientResourcePackStatus status = new WrapperPlayClientResourcePackStatus(event);
            if (event.getPlayer() == null) {
                plugin.getLogger().warning("Unable to get player for resource pack status!?!?");
                return;
            }

            final Player player = (Player) event.getPlayer();
            final WrapperPlayClientResourcePackStatus.Result result = status.getResult();
            final UUID packId = status.getPackId();
            Bukkit.getPluginManager().callEvent(new MultiVersionResourcePackStatusEvent(player, packId, ResourcePackStatus.valueOf(result.name()), false));
        }
    }
}
