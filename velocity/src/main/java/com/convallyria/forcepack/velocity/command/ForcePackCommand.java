package com.convallyria.forcepack.velocity.command;

import com.convallyria.forcepack.api.permission.Permissions;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;

import java.util.UUID;

public class ForcePackCommand {

    private final ForcePackVelocity plugin;

    public ForcePackCommand(final ForcePackVelocity plugin) {
        this.plugin = plugin;
    }

    @CommandDescription("Default ForcePack command")
    @Command("vforcepack|velocityforcepack")
    public void onDefault(CommandSource sender) {
        sender.sendMessage(Component.text("ForcePack by SamB440. Type /vforcepack help for help.", NamedTextColor.GREEN));
    }

    @CommandDescription("Reloads the plugin config along with the resource pack")
    @Permission(Permissions.RELOAD)
    @Command("vforcepack|velocityforcepack reload [send]")
    public void onReload(CommandSource commandSource,
                         @Argument(description = "Whether to send the updated resource pack to players") @Default("true") boolean send) {
        final CommandSource source = plugin.getServer().getConsoleCommandSource();
        final UUID possibleUUID = commandSource.pointers().getOrDefault(Identity.UUID, null);
        final Player sender = possibleUUID == null ? null : plugin.getServer().getPlayer(possibleUUID).orElse(null);
        Component reloadMsg = Component.text("Reloading...").color(NamedTextColor.GREEN);
        source.sendMessage(reloadMsg);
        if (sender != null) sender.sendMessage(reloadMsg);
        plugin.reloadConfig();
        plugin.loadResourcePacks(sender);

        if (send) {
            for (Player player : plugin.getServer().getAllPlayers()) {
                player.getCurrentServer().ifPresent(serverConnection ->
                        plugin.getPackHandler().setPack(player, serverConnection));
            }
        }

        Component doneMsg = Component.text("Done!").color(NamedTextColor.GREEN);
        source.sendMessage(doneMsg);
        if (sender != null) sender.sendMessage(doneMsg);
    }
}
