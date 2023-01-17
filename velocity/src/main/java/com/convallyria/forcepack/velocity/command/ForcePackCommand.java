package com.convallyria.forcepack.velocity.command;

import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class ForcePackCommand implements SimpleCommand {

    private final ForcePackVelocity plugin;

    public ForcePackCommand(final ForcePackVelocity plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(final Invocation invocation) {
        final CommandSource source = plugin.getServer().getConsoleCommandSource();
        final UUID possibleUUID = invocation.source().pointers().getOrDefault(Identity.UUID, null);
        final Player sender = possibleUUID == null ? null : plugin.getServer().getPlayer(possibleUUID).orElse(null);
        Component reloadMsg = Component.text("Reloading...").color(NamedTextColor.GREEN);
        source.sendMessage(reloadMsg);
        if (sender != null) sender.sendMessage(reloadMsg);
        plugin.reloadConfig();
        plugin.loadResourcePacks(sender);

        for (Player player : plugin.getServer().getAllPlayers()) {
            player.getCurrentServer().ifPresent(serverConnection -> {
                final ServerInfo serverInfo = serverConnection.getServerInfo();
                plugin.getPackHandler().setPack(player, serverInfo);
            });
        }

        Component doneMsg = Component.text("Done!").color(NamedTextColor.GREEN);
        source.sendMessage(doneMsg);
        if (sender != null) sender.sendMessage(doneMsg);
    }

    @Override
    public boolean hasPermission(final Invocation invocation) {
        return invocation.source().hasPermission("forcepack.reload");
    }
}
