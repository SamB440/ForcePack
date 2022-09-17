package com.convallyria.forcepack.api.verification;

public class ResourcePackURLData {

    private final String urlHash, configHash;
    private final int size;

    public ResourcePackURLData(final String urlHash, final String configHash, final int size) {
        this.urlHash = urlHash;
        this.configHash = configHash;
        this.size = size;
    }

    public String getUrlHash() {
        return urlHash;
    }

    public String getConfigHash() {
        return configHash;
    }

    public int getSize() {
        return size;
    }

    public boolean match() {
        return urlHash.equalsIgnoreCase(configHash);
    }
}
