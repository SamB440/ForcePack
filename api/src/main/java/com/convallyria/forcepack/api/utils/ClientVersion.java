package com.convallyria.forcepack.api.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum ClientVersion {
    V_1_8_1_15("1.8-1.15", 50, 47),
    V_1_16_1_17("1.16-1.17", 100, 735),
    V_1_18("1.18+", 250, 757);

    private final String display;
    private final int maxSizeMB;
    private final int minProtocolVer;

    ClientVersion(String display, int maxSizeMB, int minProtocolVer) {
        this.display = display;
        this.maxSizeMB = maxSizeMB;
        this.minProtocolVer = minProtocolVer;
    }

    public String getDisplay() {
        return display;
    }

    public int getMaxSizeMB() {
        return maxSizeMB;
    }

    public int getMinProtocolVer() {
        return minProtocolVer;
    }

    public static int getMaxSizeForVersion(int version) {
        ClientVersion currentVersion = ClientVersion.V_1_18;
        //todo: this is terrible for performance, but too lazy to work out the proper way to do this right now.
        final List<ClientVersion> values = new ArrayList<>(List.of(ClientVersion.values()));
        Collections.reverse(values);
        for (ClientVersion clientVersion : values) {
            if (version <= currentVersion.minProtocolVer) {
                currentVersion = clientVersion;
            }
        }
        return currentVersion.maxSizeMB;
    }
}
