package com.convallyria.forcepack.api.resourcepack;

import com.convallyria.forcepack.api.ForcePackAPI;
import com.convallyria.forcepack.api.utils.HashingUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public abstract class ResourcePack implements IResourcePack {

	protected final ForcePackAPI plugin;
	protected final UUID uuid;
	protected final String url;
	protected final String hash;
	protected final int size;
	protected final ResourcePackVersion packVersion;

	public ResourcePack(final ForcePackAPI plugin, String url, String hash, int size, @Nullable ResourcePackVersion packVersion) {
		this.plugin = plugin;
		// This is the same method vanilla uses to generate a UUID from the server.properties url
		// We add the hash though because we support cross-version stuff
		this.uuid = UUID.nameUUIDFromBytes((url + hash).getBytes(StandardCharsets.UTF_8));
		this.url = url;
		this.hash = hash;
		this.size = size;
		this.packVersion = packVersion;
	}

	@Override
	public UUID getUUID() {
		return uuid;
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

	@Override
	public Optional<ResourcePackVersion> getVersion() {
		return Optional.ofNullable(packVersion);
	}

	public abstract void setResourcePack(UUID uuid);

	@Override
	public int hashCode() {
		return Objects.hash(uuid);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ResourcePack)) return false;
		ResourcePack that = (ResourcePack) o;
		return Objects.equals(uuid, that.uuid);
	}
}
