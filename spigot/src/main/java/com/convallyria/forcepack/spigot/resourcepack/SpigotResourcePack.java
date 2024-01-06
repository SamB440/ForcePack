package com.convallyria.forcepack.spigot.resourcepack;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.resourcepack.ResourcePackVersion;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.translation.Translations;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

public final class SpigotResourcePack extends ResourcePack {

    private final ForcePackSpigot spigotPlugin;
    private boolean hasWarned;

    public SpigotResourcePack(final ForcePackSpigot plugin, String url, String hash, int size, @Nullable ResourcePackVersion packVersion) {
        super(plugin, Bukkit.getServer().getName(), url, hash, size, packVersion);
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
        if (player == null) return;

        // Find a prompt method that is available to us
        if (spigotPlugin.getVersionNumber() >= 18) {
            spigotPlugin.log("Using 1.18 method");
            // Uh. Pain.
            try {
                player.sendResourcePacks(ResourcePackRequest.resourcePackRequest()
                        .required(spigotPlugin.getConfig().getBoolean("use-new-force-pack-screen", true))
                        .prompt(Component.join(JoinConfiguration.newlines(), Translations.PROMPT_TEXT.getProper(player)))
                        .packs(ResourcePackInfo.resourcePackInfo(getUUID(), new URI(url), hash)));
            } catch (NoSuchMethodError ignored) {
                try {
                    player.setResourcePack(getUUID(), url, getHashSum(), Translations.PROMPT_TEXT.get(player), spigotPlugin.getConfig().getBoolean("use-new-force-pack-screen", true));
                } catch (NoSuchMethodError ignored2) { // Server is not up-to-date
                    try {
                        spigotPlugin.log("Using non-UUID method");
                        player.setResourcePack(url, getHashSum(), Translations.PROMPT_TEXT.get(player), spigotPlugin.getConfig().getBoolean("use-new-force-pack-screen", true));
                    } catch (NoSuchMethodError ignored3) {
                        if (!hasWarned) {
                            spigotPlugin.getLogger().warning("Your server is not up-to-date: cannot use new ResourcePack methods.");
                            this.hasWarned = true;
                        }

                        spigotPlugin.log("Had to fallback");
                        // Fallback
                        player.setResourcePack(url, getHashSum());
                    }
                }
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        } else if (spigotPlugin.getVersionNumber() >= 11) { // 1.11 - 1.17 support
            spigotPlugin.log("Using 1.11-1.17 method");
            player.setResourcePack(url, getHashSum());
        } else { // <= 1.10 support
            spigotPlugin.log("Using <= 1.10 method");
            player.setTexturePack(url);
        }
    }
}
