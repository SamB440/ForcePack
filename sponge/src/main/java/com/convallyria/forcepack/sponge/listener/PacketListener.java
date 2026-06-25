package com.convallyria.forcepack.sponge.listener;

import com.convallyria.forcepack.sponge.ForcePackSponge;
import com.convallyria.forcepack.sponge.event.MultiVersionResourcePackStatusEvent;
import com.convallyria.forcepack.sponge.player.ForcePackSpongePlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientResourcePackStatus;
import net.kyori.adventure.resource.ResourcePackStatus;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.profile.GameProfile;

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
        if (isResourcePackStatus(event.getPacketType())) {
            final User user = event.getUser();
            final ForcePackSpongePlayer player = plugin.getForcePackPlayer(user.getUUID()).orElse(null);
            if (player == null) {
//                plugin.getLogger().warn("Unable to get player for resource pack status!?!? {}, {}", user, event.getPlayer());
                // Player isn't valid - wasn't added at auth
                return;
            }

            plugin.log("Received packet resource pack status from " + user.getName() + " (version: " + event.getServerVersion().getReleaseName() + ")");

            final WrapperPlayClientResourcePackStatus status = new WrapperPlayClientResourcePackStatus(event);
            final WrapperPlayClientResourcePackStatus.Result result = status.getResult();
            final UUID packId = status.getPackId();

            final GameProfile gameProfile = GameProfile.of(user.getUUID(), user.getName());
            final MultiVersionResourcePackStatusEvent packEvent = new MultiVersionResourcePackStatusEvent(gameProfile, player.getConnection(), packId, ResourcePackStatus.valueOf(result.name()), false, false);
            if (Sponge.eventManager().post(packEvent)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean isResourcePackStatus(PacketTypeCommon type) {
        return type == PacketType.Play.Client.RESOURCE_PACK_STATUS || type == PacketType.Configuration.Client.RESOURCE_PACK_STATUS;
    }
}
