package com.convallyria.forcepack.spigot.command;

import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import com.convallyria.forcepack.api.permission.Permissions;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.event.ForcePackReloadEvent;
import com.convallyria.forcepack.spigot.translation.Translations;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

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
    @CommandPermission(Permissions.RELOAD)
    @CommandMethod("forcepack reload")
    public void onReload(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "Reloading...");
        plugin.reloadConfig();
        plugin.reload();
        Bukkit.getPluginManager().callEvent(new ForcePackReloadEvent());
        if (!plugin.velocityMode) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.isWaiting(player)) continue;
                boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
                boolean canBypass = player.hasPermission(Permissions.BYPASS) && plugin.getConfig().getBoolean("Server.bypass-permission");
                plugin.log(player.getName() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");
                if (geyser || canBypass) continue;

                Translations.RELOADING.send(player);

                final Set<ResourcePack> resourcePacks = plugin.getPacksForVersion(player);
                plugin.addToWaiting(player.getUniqueId(), resourcePacks);
                resourcePacks.forEach(pack -> pack.setResourcePack(player.getUniqueId()));
            }
        }

        sender.sendMessage(ChatColor.GREEN + "Done!");
    }
}
