package com.convallyria.forcepack.api.resourcepack;

import com.convallyria.forcepack.api.ForcePackAPI;
import com.convallyria.forcepack.api.utils.HashingUtil;

import java.util.UUID;

public abstract class ResourcePack implements IResourcePack {

	protected final ForcePackAPI plugin;
	protected final String url;
	protected final String hash;
	protected final int size;

	public ResourcePack(final ForcePackAPI plugin, String url, String hash, int size) {
		this.plugin = plugin;
		this.url = url;
		this.hash = hash;
		this.size = size;
	}

	@Override
	public String getURL() {
		return url;
	}

	@Override
	public String getHash() {
		return hash;
	}

	@Override
	public byte[] getHashSum() {
		return HashingUtil.toByteArray(hash);
	}

	@Override
	public int getSize() {
		return size;
	}

	public abstract void setResourcePack(UUID uuid);
}
