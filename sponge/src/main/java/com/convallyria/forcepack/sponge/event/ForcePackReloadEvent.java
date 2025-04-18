package com.convallyria.forcepack.sponge.event;

import com.convallyria.forcepack.sponge.ForcePackSponge;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;

public class ForcePackReloadEvent extends AbstractEvent {

    private final Cause cause;

    public ForcePackReloadEvent() {
        this.cause = Cause.of(EventContext.empty(), ForcePackSponge.getInstance());
    }

    @Override
    public Cause cause() {
        return cause;
    }
}
