package com.convallyria.forcepack.sponge.command;

import com.convallyria.forcepack.api.permission.Permissions;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.sponge.ForcePackSponge;
import com.convallyria.forcepack.sponge.event.ForcePackReloadEvent;
import com.convallyria.forcepack.sponge.util.ProtocolUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerResourcePackRemove;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Permission;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

import java.util.Set;
import java.util.UUID;

public class ForcePackCommand {

    private final ForcePackSponge plugin;

    public ForcePackCommand(final ForcePackSponge plugin) {
        this.plugin = plugin;
    }

    @CommandDescription("Default ForcePack command")
    @Command("forcepack")
    public void onDefault(CommandCause sender) {
        sender.sendMessage(Component.text("ForcePack by SamB440. Type /forcepack help for help.", NamedTextColor.GREEN));
    }

    @CommandDescription("Reloads the plugin config along with the resource pack")
    @Permission(Permissions.RELOAD)
    @Command("forcepack reload [send]")
    public void onReload(CommandCause sender,
                         @Argument(value = "send", description = "Whether to send the updated resource pack to players") @Default("true") boolean send) {
        sender.sendMessage(Component.text("Reloading...", NamedTextColor.GREEN));
        plugin.reloadConfig();
        plugin.reload();
        PacketEvents.getAPI().getSettings().debug(plugin.debug());
        Sponge.eventManager().post(new ForcePackReloadEvent());
        if (!plugin.getConfig().node("velocity-mode").getBoolean() && send) {
            for (ServerPlayer player : Sponge.server().onlinePlayers()) {
                if (plugin.isWaiting(player)) continue;
                boolean geyser = plugin.getConfig().node("Server", "geyser").getBoolean() && GeyserUtil.isBedrockPlayer(player.uniqueId());
                boolean canBypass = player.hasPermission(Permissions.BYPASS) && plugin.getConfig().node("Server", "bypass-permission").getBoolean();
                plugin.log(player.name() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");
                if (geyser || canBypass) continue;

                player.sendMessage(Component.translatable("forcepack.reloading"));

                final Set<ResourcePack> resourcePacks = plugin.getPacksForVersion(player);
                plugin.addToWaiting(player.uniqueId(), resourcePacks);
                if (ProtocolUtil.getProtocolVersion(player) >= 765) { // 1.20.3+
                    ProtocolUtil.sendPacketBypassingVia(player, new WrapperPlayServerResourcePackRemove((UUID) null));
                }
                resourcePacks.forEach(pack -> pack.setResourcePack(player.uniqueId()));
            }
        }

        sender.sendMessage(Component.text("Done!", NamedTextColor.GREEN));
    }
}
