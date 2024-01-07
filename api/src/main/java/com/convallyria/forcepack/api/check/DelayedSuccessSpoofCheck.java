package com.convallyria.forcepack.api.check;

import com.convallyria.forcepack.api.ForcePackAPI;

import java.util.UUID;
import java.util.function.Consumer;

public class DelayedSuccessSpoofCheck extends SpoofCheck {

    private long lastAcceptStatus;

    public DelayedSuccessSpoofCheck(ForcePackAPI plugin, UUID id) {
        super(plugin, id);
    }

    @Override
    public CheckStatus receiveStatus(String status, Consumer<String> logger) {
        final long now = System.currentTimeMillis();
        if (status.equalsIgnoreCase("accepted")) {
            lastAcceptStatus = now;
        } else if (status.equalsIgnoreCase("successfully_loaded")) {
            // If the player sent a SUCCESSFULLY_LOADED status instantly after ACCEPT, kick them.
            final boolean flag = now - lastAcceptStatus < 10;
            if (flag) {
                logger.accept("Kicked player " + id + " because they are sending fake resource pack statuses (sent too fast).");
                return CheckStatus.FAILED;
            }
        }
        return CheckStatus.PASSED;
    }
}
