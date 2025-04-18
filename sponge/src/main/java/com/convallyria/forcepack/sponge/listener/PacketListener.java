package com.convallyria.forcepack.sponge.listener;

import com.convallyria.forcepack.sponge.ForcePackSponge;
import com.convallyria.forcepack.sponge.event.MultiVersionResourcePackStatusEvent;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientResourcePackStatus;
import net.kyori.adventure.resource.ResourcePackStatus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.UUID;

public class PacketListener extends PacketListenerAbstract {

    private final ForcePackSponge plugin;

    public PacketListener(ForcePackSponge plugin) {
        this.plugin = plugin;
    }

//    @Override
//    public boolean isPreVia() {
//        return true;
//    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.RESOURCE_PACK_STATUS) {
            final ServerPlayer player = Sponge.server().player(event.getUser().getUUID()).orElse(null);
            if (player == null) {
                plugin.getLogger().warn("Unable to get player for resource pack status!?!? {}, {}", event.getUser(), event.getPlayer());
                return;
            }

            plugin.log("Received packet resource pack status from " + player.name() + " (version: " + event.getServerVersion().getReleaseName() + ")");

            final WrapperPlayClientResourcePackStatus status = new WrapperPlayClientResourcePackStatus(event);
            final WrapperPlayClientResourcePackStatus.Result result = status.getResult();
            final UUID packId = status.getPackId();
            final MultiVersionResourcePackStatusEvent packEvent = new MultiVersionResourcePackStatusEvent(player, packId, ResourcePackStatus.valueOf(result.name()), false, false);
            if (Sponge.eventManager().post(packEvent)) {
                event.setCancelled(true);
            }
        }
    }
}
