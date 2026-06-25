package com.convallyria.forcepack.sponge.event;

import net.kyori.adventure.resource.ResourcePackStatus;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.network.ServerSideConnection;
import org.spongepowered.api.profile.GameProfile;

import java.util.UUID;

/**
 * Like {@link org.spongepowered.api.event.entity.living.player.ResourcePackStatusEvent} but helps with multiple version support
 */
public class MultiVersionResourcePackStatusEvent extends AbstractEvent implements Cancellable {

    private final GameProfile profile;
    private final ServerSideConnection connection;
    private final Cause cause;
    private final UUID id;
    private final ResourcePackStatus status;
    private final boolean proxy;
    private final boolean proxyRemove;
    private boolean cancel;

    public MultiVersionResourcePackStatusEvent(@NonNull final GameProfile who, @NonNull final ServerSideConnection connection,
                                               @NonNull UUID id, @NonNull ResourcePackStatus resourcePackStatus,
                                               boolean proxy, boolean proxyRemove) {
        this.profile = who;
        this.connection = connection;
        this.id = id;
        this.status = resourcePackStatus;
        this.proxy = proxy;
        this.proxyRemove = proxyRemove;
        this.cause = Cause.of(EventContext.empty(), who);
    }

    /**
     * Returns the profile involved in this event
     * @return Profile who is involved in this event
     */
    @NonNull
    public final GameProfile getProfile() {
        return profile;
    }

    /**
     * Returns the connection involved in this event
     * @return Connection involved in this event
     */
    @NonNull
    public final ServerSideConnection getConnection() {
        return connection;
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
