package com.convallyria.forcepack.spigot.resourcepack;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.translation.Translations;
import com.viaversion.viaversion.api.Via;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class SpigotResourcePack extends ResourcePack {

    private final ForcePackSpigot spigotPlugin;
    private boolean hasWarned;

    public SpigotResourcePack(final ForcePackSpigot plugin, String url, String hash) {
        super(plugin, url, hash);
        this.spigotPlugin = plugin;
    }

    @Override
    public String getServer() {
        return Bukkit.getServer().getName();
    }

    @Override
    public void setResourcePack(UUID uuid) {
        final Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;

        if (spigotPlugin.getVersionNumber() >= 18) {
            try {
                player.setResourcePack(url, getHashSum(), Translations.PROMPT_TEXT.get(player), true);
            } catch (NoSuchMethodError ignored) { // Server is not up-to-date
                if (!hasWarned) {
                    spigotPlugin.getLogger().warning("Your server is not up-to-date: cannot use new ResourcePack methods.");
                    this.hasWarned = true;
                }
            }
        } else if (spigotPlugin.getVersionNumber() >= 11) { // 1.11 - 1.17 support
            player.setResourcePack(url, getHashSum());
        } else { // <= 1.10 support
            player.setTexturePack(url);
        }
    }
}
