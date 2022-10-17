package com.convallyria.forcepack.spigot.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.translation.Translations;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("forcepack")
public class ForcePackCommand extends BaseCommand {

    private final ForcePackSpigot plugin;

    public ForcePackCommand(final ForcePackSpigot plugin) {
        this.plugin = plugin;
    }

    @Default
    public void onDefault(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "ForcePack by SamB440. Type /forcepack help for help.");
    }

    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        help.showHelp();
    }

    @Subcommand("reload")
    @Description("Reloads the plugin config along with the resource pack")
    @CommandPermission("forcepack.reload")
    public void onReload(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "Reloading...");
        plugin.reloadConfig();
        plugin.reload();
        if (!plugin.velocityMode) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getWaiting().containsKey(player.getUniqueId())) continue;
                boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
                boolean canBypass = player.hasPermission("ForcePack.bypass") && plugin.getConfig().getBoolean("Server.bypass-permission");
                plugin.log(player.getName() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");
                if (geyser || canBypass) continue;

                Translations.RELOADING.send(player);
                final ResourcePack resourcePack = plugin.getResourcePacks().get(0);
                plugin.getWaiting().put(player.getUniqueId(), resourcePack);
            }
        }

        sender.sendMessage(ChatColor.GREEN + "Done!");
    }
}
