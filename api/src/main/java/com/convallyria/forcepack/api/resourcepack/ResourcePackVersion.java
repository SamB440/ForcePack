package com.convallyria.forcepack.api.resourcepack;

public interface ResourcePackVersion {

    int min();

    int max();

    /**
     * @param versionId the pack format version to check
     * @return true if the specified version is in the range of this {@link ResourcePackVersion}
     */
    default boolean inVersion(int versionId) {
        return versionId >= min() && versionId <= max();
    }

    static ResourcePackVersion of(int min, int max) {
        return new ResourcePackVersion() {

            @Override
            public int min() {
                return min;
            }

            @Override
            public int max() {
                return max;
            }
        };
    }
}
