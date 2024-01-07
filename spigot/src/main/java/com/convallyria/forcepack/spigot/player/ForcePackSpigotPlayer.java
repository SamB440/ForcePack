package com.convallyria.forcepack.spigot.player;

import com.convallyria.forcepack.api.check.DelayedSuccessSpoofCheck;
import com.convallyria.forcepack.api.check.InvalidOrderSpoofCheck;
import com.convallyria.forcepack.api.check.SpoofCheck;
import com.convallyria.forcepack.api.player.ForcePackPlayer;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForcePackSpigotPlayer implements ForcePackPlayer {

    private final Set<ResourcePack> waitingPacks = new HashSet<>();
    private final List<SpoofCheck> checks = new ArrayList<>();

    public ForcePackSpigotPlayer(Player player) {
        checks.add(new DelayedSuccessSpoofCheck(ForcePackSpigot.getAPI(), player.getUniqueId()));
        checks.add(new InvalidOrderSpoofCheck(ForcePackSpigot.getAPI(), player.getUniqueId()));
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
