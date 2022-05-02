package com.convallyria.forcepack.api.utils;

public enum ClientVersion {
    V_1_8_1_15("1.8-1.15", 50),
    V_1_16_1_17("1.16-1.17", 100),
    V_1_18("1.18+", 250);

    private final String display;
    private final int maxSizeMB;

    ClientVersion(String display, int maxSizeMB) {
        this.display = display;
        this.maxSizeMB = maxSizeMB;
    }

    public String getDisplay() {
        return display;
    }

    public int getMaxSizeMB() {
        return maxSizeMB;
    }
}
