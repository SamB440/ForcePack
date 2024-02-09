package com.convallyria.forcepack.spigot.resourcepack;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.resourcepack.ResourcePackVersion;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.translation.Translations;
import com.convallyria.forcepack.spigot.util.ProtocolUtil;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerResourcePackSend;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.UUID;

public final class SpigotResourcePack extends ResourcePack {

    private final ForcePackSpigot spigotPlugin;

    public SpigotResourcePack(final ForcePackSpigot plugin, String url, String hash, int size, @Nullable ResourcePackVersion packVersion) {
        this(plugin, Bukkit.getServer().getName(), url, hash, size, packVersion);
    }

    public SpigotResourcePack(final ForcePackSpigot plugin, String server, String url, String hash, int size, @Nullable ResourcePackVersion packVersion) {
        super(plugin, server, url, hash, size, packVersion);
        this.spigotPlugin = plugin;
    }

    @Override
    public void setResourcePack(UUID uuid) {
        final int delay = spigotPlugin.getConfig().getInt("delay-pack-sending-by", 0);
        if (delay > 0) {
            plugin.getScheduler().executeDelayed(() -> runSetResourcePack(uuid), delay);
        } else {
            runSetResourcePack(uuid);
        }
    }

    private void runSetResourcePack(UUID uuid) {
        final Player player = Bukkit.getPlayer(uuid);
        if (player == null) return; // Either the player disconnected or this is an NPC

        spigotPlugin.getForcePackPlayer(player).ifPresent(forcePackPlayer -> {
            forcePackPlayer.getChecks().forEach(check -> check.sendPack(this));
        });

        WrapperPlayServerResourcePackSend send = new WrapperPlayServerResourcePackSend(getUUID(), url, getHash(),
                spigotPlugin.getConfig().getBoolean("use-new-force-pack-screen", true),
                Component.join(JoinConfiguration.newlines(), Translations.PROMPT_TEXT.getProper(player)));

        ProtocolUtil.sendPacketBypassingVia(player, send);
    }
}
