package com.convallyria.forcepack.spigot.command;

import com.convallyria.forcepack.api.permission.Permissions;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.event.ForcePackReloadEvent;
import com.convallyria.forcepack.spigot.translation.Translations;
import com.convallyria.forcepack.spigot.util.ProtocolUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerResourcePackRemove;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;

import java.util.Set;
import java.util.UUID;

public class ForcePackCommand {

    private final ForcePackSpigot plugin;

    public ForcePackCommand(final ForcePackSpigot plugin) {
        this.plugin = plugin;
    }

    @CommandDescription("Default ForcePack command")
    @Command("forcepack")
    public void onDefault(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "ForcePack by SamB440. Type /forcepack help for help.");
    }

    @CommandDescription("Reloads the plugin config along with the resource pack")
    @Permission(Permissions.RELOAD)
    @Command("forcepack reload [send]")
    public void onReload(CommandSender sender,
                         @Argument(description = "Whether to send the updated resource pack to players") @Default("true") boolean send) {
        sender.sendMessage(ChatColor.GREEN + "Reloading...");
        plugin.reloadConfig();
        plugin.reload();
        PacketEvents.getAPI().getSettings().debug(plugin.debug());
        Bukkit.getPluginManager().callEvent(new ForcePackReloadEvent());
        if (!plugin.velocityMode && send) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.isWaiting(player)) continue;
                boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
                boolean canBypass = player.hasPermission(Permissions.BYPASS) && plugin.getConfig().getBoolean("Server.bypass-permission");
                plugin.log(player.getName() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");
                if (geyser || canBypass) continue;

                Translations.RELOADING.send(player);

                final Set<ResourcePack> resourcePacks = plugin.getPacksForVersion(player);
                plugin.addToWaiting(player.getUniqueId(), resourcePacks);
                if (ProtocolUtil.getProtocolVersion(player) >= 765) { // 1.20.3+
                    ProtocolUtil.sendPacketBypassingVia(player, new WrapperPlayServerResourcePackRemove((UUID) null));
                }
                resourcePacks.forEach(pack -> pack.setResourcePack(player.getUniqueId()));
            }
        }

        sender.sendMessage(ChatColor.GREEN + "Done!");
    }
}
