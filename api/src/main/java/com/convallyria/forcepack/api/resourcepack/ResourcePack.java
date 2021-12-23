package com.convallyria.forcepack.api.resourcepack;

import com.convallyria.forcepack.api.ForcePackAPI;
import com.convallyria.forcepack.api.utils.HashingUtil;

import java.util.UUID;

public abstract class ResourcePack implements IResourcePack {

	protected final ForcePackAPI plugin;
	protected final String url;
	protected final String hash;

	public ResourcePack(final ForcePackAPI plugin, String url, String hash) {
		this.plugin = plugin;
		this.url = url;
		this.hash = hash;
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

	public abstract void setResourcePack(UUID uuid);
}
