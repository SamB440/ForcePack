package com.convallyria.forcepack.sponge.player;

import com.convallyria.forcepack.api.check.DelayedSuccessSpoofCheck;
import com.convallyria.forcepack.api.check.InvalidOrderSpoofCheck;
import com.convallyria.forcepack.api.check.SpoofCheck;
import com.convallyria.forcepack.api.permission.Permissions;
import com.convallyria.forcepack.api.player.ForcePackPlayer;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.sponge.ForcePackSponge;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class ForcePackSpongePlayer implements ForcePackPlayer {

    private ServerSideConnection connection;
    private final UUID uniqueId;
    private final Set<ResourcePack> waitingPacks = new HashSet<>();
    private final List<SpoofCheck> checks = new ArrayList<>();

    public ForcePackSpongePlayer(UUID uuid) {
        this.uniqueId = uuid;
        checks.add(new DelayedSuccessSpoofCheck(ForcePackSponge.getAPI(), uuid));
        checks.add(new InvalidOrderSpoofCheck(ForcePackSponge.getAPI(), uuid));
    }

    public ServerSideConnection getConnection() {
        return connection;
    }

    public void setConnection(ServerSideConnection connection) {
        this.connection = connection;
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

    public static boolean profileIsValid(ForcePackSponge plugin, GameProfile profile) {
        final PermissionService permissionService = Sponge.server().serviceProvider().permissionService();
        Subject subject = permissionService.userSubjects().subject(profile.uniqueId().toString()).orElse(null);
        if (subject == null) {
            plugin.log("Defaulting to default subject permissions");
            subject = permissionService.defaults();
        }

        final boolean geyser = plugin.getConfig().node("Server", "geyser").getBoolean()
                && GeyserUtil.isBedrockPlayer(profile.uniqueId());

        // Bypass should be handled by velocity instead.
        final boolean velocityMode = plugin.getConfig().node("velocity-mode").getBoolean();
        if (velocityMode) {
            plugin.log("Velocity mode is enabled");
        }

        final boolean canBypass = !velocityMode
                && subject.hasPermission(Permissions.BYPASS)
                && plugin.getConfig().node("Server", "bypass-permission").getBoolean();

        plugin.log(profile.name().orElseThrow() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");

        return !canBypass && !geyser;
    }
}
