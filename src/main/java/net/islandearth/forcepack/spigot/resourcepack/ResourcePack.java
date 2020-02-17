package net.islandearth.forcepack.spigot.resourcepack;

import net.islandearth.forcepack.spigot.utils.HashingUtil;

public class ResourcePack {

	private String url;
	private String hash;
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
}
