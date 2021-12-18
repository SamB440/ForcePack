package net.islandearth.forcepack.spigot.resourcepack;

import net.islandearth.forcepack.spigot.ForcePack;
import net.islandearth.forcepack.spigot.utils.HashingUtil;
import org.bukkit.entity.Player;

public abstract class ResourcePack {

	protected final ForcePack plugin;
	protected final String url;
	protected final String hash;

	public String getUrl() {
		return url;
	}

	public String getHash() {
		return hash;
	}

	public ResourcePack(final ForcePack plugin, String url, String hash) {
		this.plugin = plugin;
		this.url = url;
		this.hash = hash;
	}
	
	public byte[] getHashHex() {
		return HashingUtil.toByteArray(hash);
	}

	public abstract void setResourcePack(Player player);
}
