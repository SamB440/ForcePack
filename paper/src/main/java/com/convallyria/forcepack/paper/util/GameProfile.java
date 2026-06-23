package com.convallyria.forcepack.paper.util;

import java.util.UUID;

public final class GameProfile {

    private final UUID uuid;
    private final String name;

    public GameProfile(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uniqueId() {
        return uuid;
    }

    public String name() {
        return name;
    }
}
