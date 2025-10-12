package com.convallyria.forcepack.api.resourcepack;

public interface ResourcePackVersion {

    double min();

    double max();

    /**
     * @param versionId the pack format version to check
     * @return true if the specified version is in the range of this {@link ResourcePackVersion}
     */
    default boolean inVersion(double versionId) {
        return versionId >= min() && versionId <= max();
    }

    static ResourcePackVersion of(double min, double max) {
        return new ResourcePackVersion() {

            @Override
            public double min() {
                return min;
            }

            @Override
            public double max() {
                return max;
            }
        };
    }
}
