package net.islandearth.forcepack.spigot.resourcepack;

import net.islandearth.forcepack.spigot.ForcePack;
import net.islandearth.forcepack.spigot.translation.Translations;
import org.bukkit.entity.Player;

public class SpigotResourcePack extends ResourcePack {

    public SpigotResourcePack(final ForcePack plugin, String url, String hash) {
        super(plugin, url, hash);
    }

    @Override
    public void setResourcePack(Player player) {
        if (plugin.getVersionNumber() >= 18) {
            player.setResourcePack(url, getHashHex(), Translations.PROMPT_TEXT.get(player), true);
        } else {
            player.setResourcePack(url, getHashHex());
        }
    }
}
