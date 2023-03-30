package com.convallyria.forcepack.spigot.resourcepack;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.translation.Translations;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class SpigotResourcePack extends ResourcePack {

    private final ForcePackSpigot spigotPlugin;
    private boolean hasWarned;

    public SpigotResourcePack(final ForcePackSpigot plugin, String url, String hash, int size) {
        super(plugin, url, hash, size);
        this.spigotPlugin = plugin;
    }

    @Override
    public String getServer() {
        return Bukkit.getServer().getName();
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
            try {
                player.setResourcePack(url, getHashSum(), Translations.PROMPT_TEXT.get(player), spigotPlugin.getConfig().getBoolean("use-new-force-pack-screen", true));
            } catch (NoSuchMethodError ignored) { // Server is not up-to-date
                if (!hasWarned) {
                    spigotPlugin.getLogger().warning("Your server is not up-to-date: cannot use new ResourcePack methods.");
                    this.hasWarned = true;
                }

                spigotPlugin.log("Had to fallback");
                // Fallback
                player.setResourcePack(url, getHashSum());
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
