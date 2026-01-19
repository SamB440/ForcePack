package com.convallyria.forcepack.api.resourcepack;

import com.convallyria.forcepack.api.ForcePackAPI;
import com.convallyria.forcepack.api.utils.HashingUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;

public abstract class ResourcePack implements IResourcePack {

	protected final ForcePackAPI plugin;
	protected final UUID uuid;
	protected final String server;
	protected final String url;
	protected final String hash;
	protected final int size;
	protected final ResourcePackVersion packVersion;

	public ResourcePack(final ForcePackAPI plugin, String server, String url, String hash, int size, @Nullable ResourcePackVersion packVersion) {
		this.plugin = plugin;
		this.server = server;
		// This is the same method vanilla uses to generate a UUID from the server.properties url
		// We add the hash and server though because we support cross-version and cross-server stuff
		this.uuid = UUID.nameUUIDFromBytes((url + hash).getBytes(StandardCharsets.UTF_8));
		this.url = url;
		this.hash = hash;
		this.size = size;
		this.packVersion = packVersion;
	}

	@Override
	public String getServer() {
		return server;
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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ResourcePack)) return false;
		ResourcePack that = (ResourcePack) o;
		return Objects.equals(uuid, that.uuid)
				&& Objects.equals(getServer(), that.getServer())
				&& sameVersion(packVersion, that.packVersion);
	}

	@Override
	public int hashCode() {
		return Objects.hash(uuid, getServer(), versionHash(packVersion));
	}

	private static boolean sameVersion(@Nullable ResourcePackVersion left, @Nullable ResourcePackVersion right) {
		if (left == right) return true;
		if (left == null || right == null) return false;
		return Double.compare(left.min(), right.min()) == 0 && Double.compare(left.max(), right.max()) == 0;
	}

	private static int versionHash(@Nullable ResourcePackVersion version) {
		if (version == null) return 0;
		return Objects.hash(version.min(), version.max());
	}

    @Override
    public String toString() {
        return new StringJoiner(", ", ResourcePack.class.getSimpleName() + "[", "]")
                .add("uuid=" + uuid)
                .add("server='" + server + "'")
                .add("url='" + url + "'")
                .add("hash='" + hash + "'")
                .add("size=" + size)
                .add("packVersion=" + packVersion)
                .toString();
    }
}
