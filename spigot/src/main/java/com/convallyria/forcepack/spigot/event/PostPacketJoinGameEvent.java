package com.convallyria.forcepack.spigot.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PostPacketJoinGameEvent extends Event {

    private final Player player;

    public PostPacketJoinGameEvent(Player player) {
        super(true);
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }
}
