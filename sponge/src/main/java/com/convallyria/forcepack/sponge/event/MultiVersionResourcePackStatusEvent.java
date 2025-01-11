package com.convallyria.forcepack.sponge.event;

import net.kyori.adventure.resource.ResourcePackStatus;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;

import java.util.UUID;

/**
 * Like {@link org.spongepowered.api.event.entity.living.player.ResourcePackStatusEvent} but helps with multiple version support
 */
public class MultiVersionResourcePackStatusEvent extends AbstractEvent implements Cancellable {

    private final ServerPlayer player;
    private final Cause cause;
    private final UUID id;
    private final ResourcePackStatus status;
    private final boolean proxy;
    private final boolean proxyRemove;
    private boolean cancel;

    public MultiVersionResourcePackStatusEvent(@NonNull final ServerPlayer who, @NonNull UUID id, @NonNull ResourcePackStatus resourcePackStatus, boolean proxy, boolean proxyRemove) {
        this.player = who;
        this.id = id;
        this.status = resourcePackStatus;
        this.proxy = proxy;
        this.proxyRemove = proxyRemove;
        this.cause = Cause.of(EventContext.empty(), who);
    }

    /**
     * Returns the player involved in this event
     * @return Player who is involved in this event
     */
    @NonNull
    public final ServerPlayer getPlayer() {
        return player;
    }

    /**
     * Gets the unique ID of this pack.
     * @return unique resource pack ID.
     */
    @Nullable
    public UUID getID() {
        return id;
    }

    /**
     * Gets the status of this pack.
     * @return the current status
     */
    @NonNull
    public ResourcePackStatus getStatus() {
        return status;
    }

    /**
     * Gets whether this event was fired by the proxy or not.
     * @return whether the proxy caused this event to be fired
     */
    public boolean isProxy() {
        return proxy;
    }

    /**
     * Gets whether the proxy has indicated the player is no longer waiting.
     * @return whether the proxy has requested the removal of the player from waiting
     */
    public boolean isProxyRemove() {
        return proxyRemove;
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    public Cause cause() {
        return cause;
    }
}
