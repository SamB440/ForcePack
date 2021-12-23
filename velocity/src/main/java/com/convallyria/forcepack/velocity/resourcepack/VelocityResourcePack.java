package com.convallyria.forcepack.velocity.resourcepack;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.UUID;

public final class VelocityResourcePack extends ResourcePack {

    private final ForcePackVelocity velocityPlugin;
    private final String server;

    public VelocityResourcePack(final ForcePackVelocity plugin, final String server, String url, String hash) {
        super(plugin, url, hash);
        this.velocityPlugin = plugin;
        this.server = server;
    }

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public void setResourcePack(UUID uuid) {
        final Optional<Player> player = velocityPlugin.getServer().getPlayer(uuid);
        if (player.isEmpty()) return;

        final ResourcePackInfo.Builder infoBuilder = velocityPlugin.getServer()
                .createResourcePackBuilder(getURL())
                .setHash(getHashSum())
                .setShouldForce(true);

        final VelocityConfig serverConfig = velocityPlugin.getConfig().getConfig("servers").getConfig(server);
        if (serverConfig != null) {
            final String promptText = serverConfig.getConfig("resourcepack").getString("prompt");
            final Component promptComponent = velocityPlugin.getMiniMessage().parse(promptText);
            infoBuilder.setPrompt(promptComponent);
        }
        player.get().sendResourcePackOffer(infoBuilder.build());
    }
}
