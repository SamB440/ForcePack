package com.convallyria.forcepack.spigot.command;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.translation.Translations;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForcePackCommand {

    private final ForcePackSpigot plugin;

    public ForcePackCommand(final ForcePackSpigot plugin) {
        this.plugin = plugin;
    }

    @CommandDescription("Default ForcePack command")
    @CommandMethod("forcepack")
    public void onDefault(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "ForcePack by SamB440. Type /forcepack help for help.");
    }

    @CommandDescription("Reloads the plugin config along with the resource pack")
    @CommandPermission("forcepack.reload")
    @CommandMethod("forcepack reload")
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
                resourcePack.setResourcePack(player.getUniqueId());
            }
        }

        sender.sendMessage(ChatColor.GREEN + "Done!");
    }
}
