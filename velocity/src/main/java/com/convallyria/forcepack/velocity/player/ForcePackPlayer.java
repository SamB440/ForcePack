package com.convallyria.forcepack.velocity.player;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @deprecated To be removed when Velocity finishes resource pack API
 */
@Deprecated(forRemoval = true)
public class ForcePackPlayer {

    private final Player player;
    private final List<ResourcePackInfo> appliedResourcePacks;

    public ForcePackPlayer(Player player) {
        this.player = player;
        this.appliedResourcePacks = new ArrayList<>();
    }

    public List<ResourcePackInfo> getAppliedResourcePacks() {
        if (player.getProtocolVersion().getProtocol() < 765) {
            return player.getAppliedResourcePack() == null ? List.of() : List.of(player.getAppliedResourcePack());
        }
        return List.copyOf(appliedResourcePacks);
    }

    public void addAppliedPack(ResourcePackInfo info) {
        if (player.getProtocolVersion().getProtocol() < 765) {
            return;
        }
        if (appliedResourcePacks.stream().anyMatch(pack -> pack.getId().equals(info.getId()))) return;
        appliedResourcePacks.add(info);
    }

    public void removeAppliedPack(ResourcePackInfo info) {
        if (player.getProtocolVersion().getProtocol() < 765) {
            return;
        }
        appliedResourcePacks.removeIf(info1 -> info1.getId().equals(info.getId())); // unsure if we can reference the object itself
    }

    public void removeAppliedPacks() {
        appliedResourcePacks.clear();
    }
}
