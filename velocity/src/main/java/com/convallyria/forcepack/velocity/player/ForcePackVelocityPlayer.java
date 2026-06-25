package com.convallyria.forcepack.velocity.player;

import com.convallyria.forcepack.api.check.DelayedSuccessSpoofCheck;
import com.convallyria.forcepack.api.check.InvalidOrderSpoofCheck;
import com.convallyria.forcepack.api.check.SpoofCheck;
import com.convallyria.forcepack.api.player.ForcePackPlayer;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.velocity.ForcePackVelocity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ForcePackVelocityPlayer implements ForcePackPlayer {

    private final UUID uniqueId;
    private final Set<ResourcePack> waitingPacks = new HashSet<>();
    private final List<SpoofCheck> checks = new ArrayList<>();

    public ForcePackVelocityPlayer(ForcePackVelocity plugin, UUID uuid) {
        this.uniqueId = uuid;
        checks.add(new DelayedSuccessSpoofCheck(plugin, uuid));
        checks.add(new InvalidOrderSpoofCheck(plugin, uuid));
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
