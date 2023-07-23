package com.convallyria.forcepack.velocity.resourcepack;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class VelocityResourcePack extends ResourcePack {

    private final ForcePackVelocity velocityPlugin;
    private final String server;
    private final @Nullable String group;

    public VelocityResourcePack(final ForcePackVelocity plugin, final String server, String url, String hash, int size, String group) {
        super(plugin, url, hash, size);
        this.velocityPlugin = plugin;
        this.server = server;
        this.group = group;
    }

    @Override
    public String getServer() {
        return server;
    }

    public @Nullable String getGroup() {
        return group;
    }

    @Override
    public void setResourcePack(UUID uuid) {
        final int delay = velocityPlugin.getConfig().getInt("delay-pack-sending-by");
        if (delay > 0) {
            velocityPlugin.getScheduler().executeDelayed(() -> runSetResourcePack(uuid), delay);
        } else {
            runSetResourcePack(uuid);
        }
    }

    private void runSetResourcePack(UUID uuid) {
        final Optional<Player> player = velocityPlugin.getServer().getPlayer(uuid);
        if (player.isEmpty()) return;

        final ResourcePackInfo.Builder infoBuilder = velocityPlugin.getServer()
                .createResourcePackBuilder(getURL())
                .setHash(getHashSum())
                .setShouldForce(velocityPlugin.getConfig().getBoolean("use-new-force-pack-screen", true));

        final VelocityConfig serverConfig;
        if (server.equals(ForcePackVelocity.GLOBAL_SERVER_NAME)) {
            serverConfig = velocityPlugin.getConfig().getConfig("global-pack");
            final List<String> excluded = serverConfig.getStringList("exclude");
            final Optional<ServerConnection> currentServer = player.get().getCurrentServer();
            if (currentServer.isPresent()) {
                if (excluded.contains(currentServer.get().getServerInfo().getName())) return;
            } else {
                ((ForcePackVelocity) plugin).log("Unable to check global resource pack exclusion list as player is not in a server!?");
            }
        } else {
            serverConfig = velocityPlugin.getConfig().getConfig("servers").getConfig(server);
        }

        if (serverConfig != null) {
            final String promptText = serverConfig.getConfig("resourcepack").getString("prompt");
            final Component promptComponent = velocityPlugin.getMiniMessage().deserialize(promptText);
            infoBuilder.setPrompt(promptComponent);
        }
        player.get().sendResourcePackOffer(infoBuilder.build());
    }
}
