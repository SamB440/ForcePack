package net.islandearth.forcepack.spigot.resourcepack;

import lombok.Getter;
import lombok.Setter;
import net.islandearth.forcepack.spigot.utils.HashingUtil;

public class ResourcePack {

	@Getter
	private String url;
	
	@Getter
	private String hash;
	
	@Getter
	@Setter
	private boolean isPromptOpen;
	
	public ResourcePack(String url, String hash) {
		this.url = url;
		this.hash = hash;
		this.isPromptOpen = true;
	}
	
	public byte[] getHashHex() {
		return HashingUtil.toByteArray(hash);
	}
}
