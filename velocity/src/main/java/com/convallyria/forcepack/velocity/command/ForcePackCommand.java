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

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;

import java.io.File;
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
                         @Argument(value = "send", description = "Whether to send the updated resource pack to players") @Default("true") boolean send) {
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

    @CommandDescription("Updates the resource pack hash (and optional URL), saves config, and reloads packs for players")
    @Permission(Permissions.RELOAD)
    @Command("vforcepack|velocityforcepack update <hash> [url]")
    public void onUpdate(CommandSource commandSource,
                         @Argument(value = "hash", description = "The new SHA-1 hash") String newHash,
                         @Argument(value = "url", description = "Optional new pack URL") @Default("") String newUrl) {
        final CommandSource source = plugin.getServer().getConsoleCommandSource();
        final UUID possibleUUID = commandSource.pointers().getOrDefault(Identity.UUID, null);
        final Player sender = possibleUUID == null ? null : plugin.getServer().getPlayer(possibleUUID).orElse(null);

        String cleanHash = newHash.trim();
        if (cleanHash.startsWith("#")) {
            cleanHash = cleanHash.substring(1);
        }

        Component startMsg = Component.text("Updating resource pack hash to " + cleanHash + "...").color(NamedTextColor.YELLOW);
        source.sendMessage(startMsg);
        if (sender != null) sender.sendMessage(startMsg);

        try {
            File file = new File(plugin.getDataDirectory() + File.separator + "config.toml");
            CommentedFileConfig conf = CommentedFileConfig.builder(file, TomlFormat.instance()).preserveInsertionOrder().build();
            conf.load();

            // Update in global-pack if present
            if (conf.contains("global-pack.hash")) {
                conf.set("global-pack.hash", cleanHash);
                if (!newUrl.isEmpty()) {
                    conf.set("global-pack.url", newUrl);
                }
            }

            conf.save();
            conf.close();

            plugin.getLogger().info("Saved new hash {} to config.toml", cleanHash);

            plugin.reloadConfig();
            plugin.loadResourcePacks(sender);

            for (Player player : plugin.getServer().getAllPlayers()) {
                player.getCurrentServer().ifPresent(serverConnection ->
                        plugin.getPackHandler().setPack(player, serverConnection));
            }

            Component doneMsg = Component.text("ForcePack hash updated to " + cleanHash + " and packs sent!").color(NamedTextColor.GREEN);
            source.sendMessage(doneMsg);
            if (sender != null) sender.sendMessage(doneMsg);

        } catch (Exception e) {
            plugin.getLogger().error("Failed to update config.toml: {}", e.getMessage(), e);
            Component errMsg = Component.text("Failed to update ForcePack: " + e.getMessage()).color(NamedTextColor.RED);
            source.sendMessage(errMsg);
            if (sender != null) sender.sendMessage(errMsg);
        }
    }
}
