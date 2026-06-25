package com.convallyria.forcepack.paper.player;

import com.convallyria.forcepack.api.check.DelayedSuccessSpoofCheck;
import com.convallyria.forcepack.api.check.InvalidOrderSpoofCheck;
import com.convallyria.forcepack.api.check.SpoofCheck;
import com.convallyria.forcepack.api.player.ForcePackPlayer;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.paper.ForcePackPaper;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDisconnect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ForcePackPaperPlayer implements ForcePackPlayer {

    private final UUID uniqueId;
    private final Set<ResourcePack> waitingPacks = new HashSet<>();
    private final List<SpoofCheck> checks = new ArrayList<>();

    public ForcePackPaperPlayer(UUID uuid) {
        this.uniqueId = uuid;
        checks.add(new DelayedSuccessSpoofCheck(ForcePackPaper.getAPI(), uuid));
        checks.add(new InvalidOrderSpoofCheck(ForcePackPaper.getAPI(), uuid));
    }

    public void closeConnection(Component reason) {
        final Object channel = PacketEvents.getAPI().getProtocolManager().getChannel(uniqueId);
        final User user = PacketEvents.getAPI().getProtocolManager().getUser(channel);
        user.writePacket(new WrapperPlayServerDisconnect(reason));
        user.closeConnection();
    }

    public Optional<Player> player() {
        return Optional.ofNullable(Bukkit.getPlayer(uniqueId));
    }

    @Override
    public UUID uniqueId() {
        return uniqueId;
    }

    @Override
    public Set<ResourcePack> getWaitingPacks() {
        return waitingPacks;
    }

    @Override
    public List<SpoofCheck> getChecks() {
        return checks;
    }
}
