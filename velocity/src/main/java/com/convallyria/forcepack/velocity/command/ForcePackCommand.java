package com.convallyria.forcepack.velocity.command;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class ForcePackCommand {

    private final ForcePackVelocity plugin;

    public ForcePackCommand(final ForcePackVelocity plugin) {
        this.plugin = plugin;
    }

    @CommandDescription("Default ForcePack command")
    @CommandMethod("vforcepack|velocityforcepack")
    public void onDefault(CommandSource sender) {
        sender.sendMessage(Component.text("ForcePack by SamB440. Type /vforcepack help for help.", NamedTextColor.GREEN));
    }

    @CommandDescription("Reloads the plugin config along with the resource pack")
    @CommandPermission("forcepack.reload")
    @CommandMethod("vforcepack|velocityforcepack reload")
    public void onReload(CommandSource commandSource) {
        final CommandSource source = plugin.getServer().getConsoleCommandSource();
        final UUID possibleUUID = commandSource.pointers().getOrDefault(Identity.UUID, null);
        final Player sender = possibleUUID == null ? null : plugin.getServer().getPlayer(possibleUUID).orElse(null);
        Component reloadMsg = Component.text("Reloading...").color(NamedTextColor.GREEN);
        source.sendMessage(reloadMsg);
        if (sender != null) sender.sendMessage(reloadMsg);
        plugin.reloadConfig();
        plugin.loadResourcePacks(sender);

        for (Player player : plugin.getServer().getAllPlayers()) {
            player.getCurrentServer().ifPresent(serverConnection ->
                    plugin.getPackHandler().setPack(player, serverConnection));
        }

        Component doneMsg = Component.text("Done!").color(NamedTextColor.GREEN);
        source.sendMessage(doneMsg);
        if (sender != null) sender.sendMessage(doneMsg);
    }
}
