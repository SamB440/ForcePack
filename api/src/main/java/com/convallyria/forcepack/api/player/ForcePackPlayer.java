package com.convallyria.forcepack.api.player;

import com.convallyria.forcepack.api.check.SpoofCheck;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface ForcePackPlayer {

    UUID uniqueId();

    Set<ResourcePack> getWaitingPacks();

    List<SpoofCheck> getChecks();
}
