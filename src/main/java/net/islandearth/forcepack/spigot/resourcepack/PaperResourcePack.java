package net.islandearth.forcepack.spigot.resourcepack;

import net.islandearth.forcepack.spigot.translation.Translations;
import net.islandearth.forcepack.spigot.utils.HashingUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PaperResourcePack extends ResourcePack {

    public PaperResourcePack(String url, String hash) {
        super(url, hash);
    }

    @Override
    public void setResourcePack(Player player) {
        if (Bukkit.getBukkitVersion().contains("1.17")) {
            final Component text = LegacyComponentSerializer.legacyAmpersand().deserialize(Translations.PROMPT_TEXT.get(player));
            player.setResourcePack(url, hash, true, text);
        } else {
            player.setResourcePack(url, HashingUtil.toByteArray(hash));
        }
    }
}
