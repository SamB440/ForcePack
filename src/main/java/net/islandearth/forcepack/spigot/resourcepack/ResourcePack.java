package net.islandearth.forcepack.spigot.resourcepack;

import net.islandearth.forcepack.spigot.utils.HashingUtil;
import org.bukkit.entity.Player;

public abstract class ResourcePack {

	protected final String url;
	protected final String hash;
	private boolean isPromptOpen;

	public String getUrl() {
		return url;
	}

	public String getHash() {
		return hash;
	}

	public boolean isPromptOpen() {
		return isPromptOpen;
	}

	public void setPromptOpen(boolean promptOpen) {
		isPromptOpen = promptOpen;
	}

	public ResourcePack(String url, String hash) {
		this.url = url;
		this.hash = hash;
		this.isPromptOpen = true;
	}
	
	public byte[] getHashHex() {
		return HashingUtil.toByteArray(hash);
	}

	public abstract void setResourcePack(Player player);
}
