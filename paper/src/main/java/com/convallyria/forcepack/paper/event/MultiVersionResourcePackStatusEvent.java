package com.convallyria.forcepack.paper.event;

import com.convallyria.forcepack.paper.util.GameProfile;
import net.kyori.adventure.resource.ResourcePackStatus;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Like {@link PlayerResourcePackStatusEvent} but helps with multiple version support.
 * <p>
 * Unlike the native event, this event may be fired during the configuration phase, where no
 * {@link org.bukkit.entity.Player} object exists yet. For that reason it carries the player's profile instead.
 */
public class MultiVersionResourcePackStatusEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final GameProfile profile;
    private final UUID id;
    private final ResourcePackStatus status;
    private final boolean proxy;
    private final boolean proxyRemove;
    private boolean cancel;

    public MultiVersionResourcePackStatusEvent(@NotNull final GameProfile profile,
                                               @NotNull UUID id, @NotNull ResourcePackStatus resourcePackStatus,
                                               boolean proxy, boolean proxyRemove) {
        super(true);
        this.profile = profile;
        this.id = id;
        this.status = resourcePackStatus;
        this.proxy = proxy;
        this.proxyRemove = proxyRemove;
    }

    public GameProfile getProfile() {
        return profile;
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
    @NotNull
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

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
