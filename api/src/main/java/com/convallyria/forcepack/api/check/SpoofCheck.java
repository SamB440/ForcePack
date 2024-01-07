package com.convallyria.forcepack.api.check;

import com.convallyria.forcepack.api.ForcePackAPI;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;

import java.util.UUID;
import java.util.function.Consumer;

public class SpoofCheck {

    protected final ForcePackAPI plugin;
    protected final UUID id;

    public SpoofCheck(ForcePackAPI plugin, UUID id) {
        this.plugin = plugin;
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public void sendPack(ResourcePack pack) {

    }

    public CheckStatus receiveStatus(String status, Consumer<String> logger) {
        return CheckStatus.PASSED;
    }

    public enum CheckStatus {
        FAILED,
        CANCEL,
        PASSED
    }
}
