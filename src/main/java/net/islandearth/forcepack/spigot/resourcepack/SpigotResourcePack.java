package net.islandearth.forcepack.spigot.resourcepack;

import org.bukkit.entity.Player;

public class SpigotResourcePack extends ResourcePack {

    public SpigotResourcePack(String url, String hash) {
        super(url, hash);
    }

    @Override
    public void setResourcePack(Player player) {
        player.setResourcePack(url, getHashHex());
    }
}
