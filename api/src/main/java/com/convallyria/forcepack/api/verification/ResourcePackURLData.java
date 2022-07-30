package com.convallyria.forcepack.api.verification;

public class ResourcePackURLData {

    private final String urlHash, configHash;

    public ResourcePackURLData(final String urlHash, final String configHash) {
        this.urlHash = urlHash;
        this.configHash = configHash;
    }

    public String getUrlHash() {
        return urlHash;
    }

    public String getConfigHash() {
        return configHash;
    }

    public boolean match() {
        return urlHash.equalsIgnoreCase(configHash);
    }
}
