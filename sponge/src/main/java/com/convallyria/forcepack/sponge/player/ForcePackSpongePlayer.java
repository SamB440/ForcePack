package com.convallyria.forcepack.sponge.player;

import com.convallyria.forcepack.api.check.DelayedSuccessSpoofCheck;
import com.convallyria.forcepack.api.check.InvalidOrderSpoofCheck;
import com.convallyria.forcepack.api.check.SpoofCheck;
import com.convallyria.forcepack.api.player.ForcePackPlayer;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.sponge.ForcePackSponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForcePackSpongePlayer implements ForcePackPlayer {

    private final Set<ResourcePack> waitingPacks = new HashSet<>();
    private final List<SpoofCheck> checks = new ArrayList<>();

    public ForcePackSpongePlayer(ServerPlayer player) {
        checks.add(new DelayedSuccessSpoofCheck(ForcePackSponge.getAPI(), player.uniqueId()));
        checks.add(new InvalidOrderSpoofCheck(ForcePackSponge.getAPI(), player.uniqueId()));
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
