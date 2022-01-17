package com.convallyria.forcepack.velocity.command;

import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public record ForcePackCommand(ForcePackVelocity plugin) implements SimpleCommand {

    @Override
    public void execute(final Invocation invocation) {
        final CommandSource source = invocation.source();
        source.sendMessage(Component.text("Reloading...").color(NamedTextColor.GREEN));
        plugin.loadResourcePacks();
        for (Player player : plugin.getServer().getAllPlayers()) {
            player.getCurrentServer().ifPresent(serverConnection -> {
                final ServerInfo serverInfo = serverConnection.getServerInfo();
                plugin.getPackHandler().setPack(player, serverInfo);
            });
        }
        source.sendMessage(Component.text("Done!").color(NamedTextColor.GREEN));
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("forcepack.reload");
    }
}
