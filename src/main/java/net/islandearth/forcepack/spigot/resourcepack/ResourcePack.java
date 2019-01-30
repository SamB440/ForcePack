package net.islandearth.forcepack.spigot.resourcepack;

import lombok.Getter;
import net.islandearth.forcepack.spigot.utils.HashingUtil;

public class ResourcePack {

	@Getter
	private String url;
	
	@Getter
	private String hash;
	
	public ResourcePack(String url, String hash) {
		this.url = url;
		this.hash = hash;
	}
	
	public byte[] getHashHex() {
		return HashingUtil.toByteArray(hash);
	}
}
