package com.convallyria.forcepack.velocity.resourcepack;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.scheduler.Scheduler;
import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class VelocityResourcePack extends ResourcePack {

    private final ForcePackVelocity velocityPlugin;
    private final String server;

    public VelocityResourcePack(final ForcePackVelocity plugin, final String server, String url, String hash, int size) {
        super(plugin, url, hash, size);
        this.velocityPlugin = plugin;
        this.server = server;
    }

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public void setResourcePack(UUID uuid) {
        final int delay = velocityPlugin.getConfig().getInt("delay-pack-sending-by");
        if (delay > 0) {
            velocityPlugin.getServer().getScheduler().buildTask(plugin, () -> {
                runSetResourcePack(uuid);
            }).delay(50L * delay, TimeUnit.MILLISECONDS);
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
