package com.convallyria.forcepack.velocity;

import com.convallyria.forcepack.api.ForcePackAPI;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.HashingUtil;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.convallyria.forcepack.velocity.listener.ResourcePackListener;
import com.convallyria.forcepack.velocity.resourcepack.VelocityResourcePack;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.markdown.DiscordFlavor;
import org.bstats.velocity.Metrics;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Plugin(
        id = "forcepack",
        name = "ForcePack",
        version = "1.1.6",
        description = "Force players to use your server resource pack.",
        url = "https://www.convallyria.com",
        authors = {"SamB440"}
)
public class ForcePackVelocity implements ForcePackAPI {

    public static final String EMPTY_SERVER_NAME = "ForcePack-Empty-Server";

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;

    @Inject
    public ForcePackVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
    }

    private VelocityConfig config;
    private List<ResourcePack> resourcePacks = new ArrayList<>();

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        getLogger().info("Enabling ForcePack (velocity)...");
        this.createConfig();
        this.loadResourcePacks();
        this.registerListeners();
        metricsFactory.make(this, 13678);
    }

    private void createConfig() {
        final Path dirPath = Path.of(dataDirectory + File.separator);
        final File dirFile = dirPath.toFile();
        if (!dirFile.exists()) {
            dirFile.mkdirs();
            try {
                final Path configPath = Path.of(dataDirectory + File.separator + "config.toml");
                Files.copy(this.getClass().getResourceAsStream("/config.toml"), configPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.config = new VelocityConfig(this);
    }

    private void registerListeners() {
        final EventManager eventManager = server.getEventManager();
        eventManager.register(this, new ResourcePackListener(this));
    }

    private void loadResourcePacks() {
        final VelocityConfig unloadPack = getConfig().getConfig("unload-pack");
        final boolean enableUnload = unloadPack.getBoolean("enable");
        if (enableUnload) {
            final String url = unloadPack.getString("url");
            final String hash = unloadPack.getString("hash");
            final VelocityResourcePack resourcePack = new VelocityResourcePack(this, EMPTY_SERVER_NAME, url, hash);
            resourcePacks.add(resourcePack);
        }

        final VelocityConfig servers = getConfig().getConfig("servers");
        for (String serverName : servers.getKeys()) {
            final VelocityConfig serverConfig = servers.getConfig(serverName);
            final VelocityConfig resourcePack = serverConfig.getConfig("resourcepack");
            final String url = resourcePack.getString("url");
            final String hash = resourcePack.getString("hash");
            resourcePacks.add(new VelocityResourcePack(this, serverName, url, hash));
        }

        final boolean verifyPacks = getConfig().getBoolean("verify-resource-packs");
        if (!verifyPacks) return;
        for (ResourcePack resourcePack : ImmutableList.copyOf(resourcePacks)) {
            final String url = resourcePack.getURL();
            final String hash = resourcePack.getHash();
            final String serverName = resourcePack.getServer();
            try {
                HashingUtil.performPackCheck(url, hash, (urlBytes, hashBytes, match) -> {
                    if (!match) {
                        this.getLogger().error("-----------------------------------------------");
                        this.getLogger().error("Your hash does not match the URL file provided!");
                        this.getLogger().error("Target server: " + serverName);
                        this.getLogger().error("The URL hash returned: " + Arrays.toString(urlBytes));
                        this.getLogger().error("Your config hash returned: " + Arrays.toString(hashBytes));
                        this.getLogger().error("Please provide a correct SHA-1 hash!");
                        this.getLogger().error("-----------------------------------------------");
                        resourcePacks.remove(resourcePack);
                    } else {
                        this.getLogger().info("Loaded ResourcePack for server " + serverName + ".");
                    }
                });
            } catch (Exception e) {
                this.getLogger().error("Please provide a correct SHA-1 hash/url!");
                e.printStackTrace();
            }
        }
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public VelocityConfig getConfig() {
        return config;
    }

    @Override
    public List<ResourcePack> getResourcePacks() {
        return resourcePacks;
    }

    public Optional<ResourcePack> getPackByServer(final String server) {
        for (ResourcePack resourcePack : resourcePacks) {
            if (resourcePack.getServer().equals(server)) {
                return Optional.of(resourcePack);
            }
        }
        return Optional.empty();
    }

    private MiniMessage miniMessage;

    public MiniMessage getMiniMessage() {
        if (miniMessage != null) return this.miniMessage;
        return this.miniMessage = MiniMessage.builder()
                .markdown()
                .markdownFlavor(DiscordFlavor.get())
                .build();
    }

    public void log(String info) {
        if (this.getConfig().getBoolean("debug")) this.getLogger().info(info);
    }
}
