package com.convallyria.forcepack.velocity.player;

import com.convallyria.forcepack.api.check.DelayedSuccessSpoofCheck;
import com.convallyria.forcepack.api.check.InvalidOrderSpoofCheck;
import com.convallyria.forcepack.api.check.SpoofCheck;
import com.convallyria.forcepack.api.player.ForcePackPlayer;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.velocitypowered.api.proxy.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForcePackVelocityPlayer implements ForcePackPlayer {

    private final Set<ResourcePack> waitingPacks = new HashSet<>();
    private final List<SpoofCheck> checks = new ArrayList<>();

    public ForcePackVelocityPlayer(ForcePackVelocity plugin, Player player) {
        checks.add(new DelayedSuccessSpoofCheck(plugin, player.getUniqueId()));
        checks.add(new InvalidOrderSpoofCheck(plugin, player.getUniqueId()));
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
