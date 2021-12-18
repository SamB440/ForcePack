package net.islandearth.forcepack.spigot.resourcepack;

import net.islandearth.forcepack.spigot.ForcePack;
import net.islandearth.forcepack.spigot.translation.Translations;
import org.bukkit.entity.Player;

public class SpigotResourcePack extends ResourcePack {

    private boolean hasWarned;

    public SpigotResourcePack(final ForcePack plugin, String url, String hash) {
        super(plugin, url, hash);
    }

    @Override
    public void setResourcePack(Player player) {
        if (plugin.getVersionNumber() >= 18) {
            try {
                player.setResourcePack(url, getHashHex(), Translations.PROMPT_TEXT.get(player), true);
                return;
            } catch (NoSuchMethodError ignored) { // Server is not up-to-date
                if (!hasWarned) {
                    plugin.getLogger().warning("Your server is not up-to-date: cannot use new ResourcePack methods.");
                    this.hasWarned = true;
                }
            }
        }
        player.setResourcePack(url, getHashHex());
    }
}
