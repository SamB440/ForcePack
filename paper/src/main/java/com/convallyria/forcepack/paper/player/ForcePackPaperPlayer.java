package com.convallyria.forcepack.paper.player;

import com.convallyria.forcepack.api.check.DelayedSuccessSpoofCheck;
import com.convallyria.forcepack.api.check.InvalidOrderSpoofCheck;
import com.convallyria.forcepack.api.check.SpoofCheck;
import com.convallyria.forcepack.api.player.ForcePackPlayer;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.paper.ForcePackPaper;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForcePackPaperPlayer implements ForcePackPlayer {

    private final Set<ResourcePack> waitingPacks = new HashSet<>();
    private final List<SpoofCheck> checks = new ArrayList<>();

    public ForcePackPaperPlayer(Player player) {
        checks.add(new DelayedSuccessSpoofCheck(ForcePackPaper.getAPI(), player.getUniqueId()));
        checks.add(new InvalidOrderSpoofCheck(ForcePackPaper.getAPI(), player.getUniqueId()));
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
