package com.convallyria.forcepack.api;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;

import java.util.List;

public interface ForcePackAPI {

    /**
     * Gets the loaded {@link ResourcePack}s.
     *   These are the resource packs loaded after checks have been completed by the plugin
     *   to verify the SHA-1 hash of the provided URL download.
     * @return the loaded ResourcePacks
     */
    List<ResourcePack> getResourcePacks();

    /**
     * Gets the ForcePack API.
     * @return the forcepack API implementation
     */
    static ForcePackAPI getInstance() {
        return ForcePackImpl.Instance.getImplementation();
    }
}
