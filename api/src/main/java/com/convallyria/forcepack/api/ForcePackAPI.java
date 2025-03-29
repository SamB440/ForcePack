package com.convallyria.forcepack.api;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.schedule.PlatformScheduler;

import java.util.Set;
import java.util.UUID;

public interface ForcePackAPI {

    /**
     * Gets the loaded {@link ResourcePack}s.
     *   These are the resource packs loaded after checks have been completed by the plugin
     *   to verify the SHA-1 hash of the provided URL download.
     * @return the loaded ResourcePacks
     */
    Set<ResourcePack> getResourcePacks();

    /**
     * Gets the scheduler used for the current platform.
     * @return the scheduler for this server platform
     */
    PlatformScheduler<?> getScheduler();

    /**
     * @param uuid The UUID of the player to exempt the next resource pack send from.
     * @return true if the player was successfully exempted, false if they were already on the exemption list.
     */
    boolean exemptNextResourcePackSend(UUID uuid);
}
