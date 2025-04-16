package com.convallyria.forcepack.sponge.resourcepack;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.resourcepack.ResourcePackVersion;
import com.convallyria.forcepack.sponge.ForcePackSponge;
import com.convallyria.forcepack.sponge.util.ProtocolUtil;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerResourcePackSend;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.UUID;

public final class SpongeResourcePack extends ResourcePack {

    private final ForcePackSponge spongePlugin;

    public SpongeResourcePack(final ForcePackSponge plugin, String url, String hash, int size, @Nullable ResourcePackVersion packVersion) {
        this(plugin, "Sponge", url, hash, size, packVersion);
    }

    public SpongeResourcePack(final ForcePackSponge plugin, String server, String url, String hash, int size, @Nullable ResourcePackVersion packVersion) {
        super(plugin, server, url, hash, size, packVersion);
        this.spongePlugin = plugin;
    }

    @Override
    public void setResourcePack(UUID uuid) {
        final int delay = spongePlugin.getConfig().node("delay-pack-sending-by").getInt(0);
        if (delay > 0) {
            plugin.getScheduler().executeDelayed(() -> runSetResourcePack(uuid), delay);
        } else {
            runSetResourcePack(uuid);
        }
    }

    private void runSetResourcePack(UUID uuid) {
        final ServerPlayer player = Sponge.server().player(uuid).orElse(null);
        if (player == null) return; // Either the player disconnected or this is an NPC

        spongePlugin.getForcePackPlayer(player).ifPresent(forcePackPlayer -> {
            forcePackPlayer.getChecks().forEach(check -> check.sendPack(this));
        });

        WrapperPlayServerResourcePackSend send = new WrapperPlayServerResourcePackSend(getUUID(), url, getHash(),
                spongePlugin.getConfig().node("use-new-force-pack-screen").getBoolean(true),
                Component.join(JoinConfiguration.newlines(), Component.translatable("forcepack.prompt_text")));

        ProtocolUtil.sendPacketBypassingVia(player, send);
    }
}
