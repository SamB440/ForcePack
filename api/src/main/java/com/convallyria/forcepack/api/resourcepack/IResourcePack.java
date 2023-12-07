package com.convallyria.forcepack.api.resourcepack;

import java.util.Optional;
import java.util.UUID;

public interface IResourcePack {

    /**
     * Gets the name of the server that this ResourcePack is associated with.
     * @return name of server
     */
    public String getServer();

    /**
     * Gets the UUID sent to 1.20.3+ clients.
     * @return uuid of the resource pack
     */
    public UUID getUUID();

    /**
     * Gets the URL of this ResourcePack.
     * @return string representation of URL
     */
    public String getURL();

    /**
     * Gets the SHA-1 hash of this ResourcePack.
     * @return string representation of SHA-1 ResourcePack download.
     */
    public String getHash();

    /**
     * Gets the SHA-1 hash sum of this ResourcePack.
     * This is always 20 bytes long.
     * @return SHA-1 hash sum of the ResourcePack file.
     */
    public byte[] getHashSum();

    /**
     * Gets the size, in megabytes, of this ResourcePack.
     * @return size in megabytes of this ResourcePack
     */
    public int getSize();

    /**
     * Gets the version intended for this resource pack.
     * @return Optional containing the version intended for this resource pack, or empty if for any version.
     */
    public Optional<ResourcePackVersion> getVersion();

    /**
     * Attempts to apply the ResourcePack to the specified player.
     * This will use the appropriate method for the current platform and version.
     * @param player player's UUID
     */
    public void setResourcePack(UUID player);
}
