package com.convallyria.forcepack.api.check;

import com.convallyria.forcepack.api.ForcePackAPI;

import java.util.UUID;
import java.util.function.Consumer;

public class InvalidOrderSpoofCheck extends SpoofCheck {

    private boolean receivedAccepted;

    public InvalidOrderSpoofCheck(ForcePackAPI plugin, UUID id) {
        super(plugin, id);
    }

    @Override
    public CheckStatus receiveStatus(String status, Consumer<String> logger) {
        if (status.equalsIgnoreCase("accepted")) {
            this.receivedAccepted = true;
        } else if (status.equalsIgnoreCase("successfully_loaded")) {
            // If the player did not send an ACCEPTED status before loading, flag them.
            final boolean flag = !receivedAccepted;
            if (flag) {
                logger.accept("Kicked player " + id + " because they are sending fake resource pack statuses (did not send ACCEPTED before SUCCESSFULLY_LOADED).");
                return CheckStatus.FAILED;
            }
        }
        return CheckStatus.PASSED;
    }
}
