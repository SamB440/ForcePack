package com.convallyria.forcepack.spigot.event;

import net.kyori.adventure.resource.ResourcePackStatus;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Like {@link PlayerResourcePackStatusEvent} but helps with multiple version support
 */
public class MultiVersionResourcePackStatusEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final UUID id;
    private final ResourcePackStatus status;
    private final boolean proxy;

    public MultiVersionResourcePackStatusEvent(@NotNull final Player who, @NotNull UUID id, @NotNull ResourcePackStatus resourcePackStatus, boolean proxy) {
        super(true);
        this.player = who;
        this.id = id;
        this.status = resourcePackStatus;
        this.proxy = proxy;
    }

    /**
     * Returns the player involved in this event
     * @return Player who is involved in this event
     */
    @NotNull
    public final Player getPlayer() {
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
