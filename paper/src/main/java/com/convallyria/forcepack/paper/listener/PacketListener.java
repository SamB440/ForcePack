package com.convallyria.forcepack.paper.listener;

import com.convallyria.forcepack.paper.ForcePackPaper;
import com.convallyria.forcepack.paper.event.MultiVersionResourcePackStatusEvent;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientResourcePackStatus;
import net.kyori.adventure.resource.ResourcePackStatus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PacketListener extends PacketListenerAbstract {

    private final ForcePackPaper plugin;

    public PacketListener(ForcePackPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isPreVia() {
        return true;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.RESOURCE_PACK_STATUS) {
            final Player player = Bukkit.getPlayer(event.getUser().getUUID());
            if (player == null) {
                plugin.getLogger().warning("Unable to get player for resource pack status!?!? " + event.getUser() + ", " + event.getPlayer());
                return;
            }

            plugin.log("Received packet resource pack status from " + player.getName() + " (version: " + event.getServerVersion().getReleaseName() + ")");

            final WrapperPlayClientResourcePackStatus status = new WrapperPlayClientResourcePackStatus(event);
            final WrapperPlayClientResourcePackStatus.Result result = status.getResult();
            final UUID packId = status.getPackId();
            final MultiVersionResourcePackStatusEvent packEvent = new MultiVersionResourcePackStatusEvent(player, packId, ResourcePackStatus.valueOf(result.name()), false, false);
            Bukkit.getPluginManager().callEvent(packEvent);
            event.setCancelled(packEvent.isCancelled());
        }
    }
}
