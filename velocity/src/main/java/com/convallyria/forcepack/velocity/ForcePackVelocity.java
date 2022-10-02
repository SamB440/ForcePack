package com.convallyria.forcepack.velocity;

import com.convallyria.forcepack.api.ForcePackAPI;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.api.utils.HashingUtil;
import com.convallyria.forcepack.api.verification.ResourcePackURLData;
import com.convallyria.forcepack.velocity.command.ForcePackCommand;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.convallyria.forcepack.velocity.handler.PackHandler;
import com.convallyria.forcepack.velocity.listener.ResourcePackListener;
import com.convallyria.forcepack.velocity.resourcepack.VelocityResourcePack;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.velocity.Metrics;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Plugin(
        id = "forcepack",
        name = "ForcePack",
        version = "1.2.4",
        description = "Force players to use your server resource pack.",
        url = "https://www.convallyria.com",
        authors = {"SamB440"}
)
public class ForcePackVelocity implements ForcePackAPI {

    public static final String EMPTY_SERVER_NAME = "ForcePack-Empty-Server";
    public static final String GLOBAL_SERVER_NAME = "ForcePack-Global-Server";

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;
    private final CommandManager commandManager;

    @Inject
    public ForcePackVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory, CommandManager commandManager) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
        this.commandManager = commandManager;
    }

    private VelocityConfig config;
    private PackHandler packHandler;
    private @Nullable ResourcePack globalResourcePack;
    private final List<ResourcePack> resourcePacks = new ArrayList<>();

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        getLogger().info("Enabling ForcePack (velocity)...");
        this.createConfig();
        this.packHandler = new PackHandler(this);
        this.loadResourcePacks();
        this.registerListeners();
        this.registerCommands();
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

        if (!getConfig().getBoolean("disable-commands-until-loaded", false)) return;
        eventManager.register(this, CommandExecuteEvent.class, event -> {
            if (event.getCommandSource() instanceof Player) {
                Player player = (Player) event.getCommandSource();
                final String command = event.getCommand();
                if (getConfig().getStringList("exclude-commands").contains(command)) return;
                if (packHandler.getApplying().contains(player.getUniqueId())) {
                    log("Stopping command '%s' because player has not loaded the resource pack yet.", command);
                    event.setResult(CommandExecuteEvent.CommandResult.denied());
                }
            }
        });
    }

    private void registerCommands() {
        CommandMeta meta = commandManager.metaBuilder("forcepackreload").build();
        commandManager.register(meta, new ForcePackCommand(this));
    }

    public void loadResourcePacks() {
        resourcePacks.clear(); // Clear for reloads

        this.checkUnload();
        this.checkGlobal();

        final boolean verifyPacks = getConfig().getBoolean("verify-resource-packs");
        final VelocityConfig servers = getConfig().getConfig("servers");
        for (String serverName : servers.getKeys()) {
            final VelocityConfig serverConfig = servers.getConfig(serverName);
            final VelocityConfig resourcePack = serverConfig.getConfig("resourcepack");
            String url = resourcePack.getString("url");
            String hash = resourcePack.getString("hash", "");
            AtomicInteger sizeInMB = new AtomicInteger();

            this.checkValidEnding(url);
            this.checkForRehost(url, serverName);

            ResourcePackURLData data = this.tryGenerateHash(resourcePack, url, hash, sizeInMB);
            if (data != null) hash = data.getUrlHash();

            if (getConfig().getBoolean("enable-mc-164316-fix", false)) {
                url = url + "#" + hash;
            }

            if (verifyPacks) {
                try {
                    Consumer<Integer> consumer = (size) -> {
                        getLogger().info("Performing version size check...");
                        for (ClientVersion clientVersion : ClientVersion.values()) {
                            String sizeStr = clientVersion.getDisplay() + " (" + clientVersion.getMaxSizeMB() + " MB): ";
                            if (clientVersion.getMaxSizeMB() < size) {
                                logger.info(sizeStr + "Unsupported.");
                            } else {
                                logger.info(sizeStr + "Supported.");
                            }
                        }

                        sizeInMB.set(size);
                    };

                    if (data == null) {
                        data = HashingUtil.performPackCheck(url, hash);
                    }

                    consumer.accept(data.getSize());

                    if (!hash.equals(data.getUrlHash())) {
                        this.getLogger().error("-----------------------------------------------");
                        this.getLogger().error("Your hash does not match the URL file provided!");
                        this.getLogger().error("Target server: " + serverName);
                        this.getLogger().error("The URL hash returned: " + data.getUrlHash());
                        this.getLogger().error("Your config hash returned: " + data.getConfigHash());
                        this.getLogger().error("Please provide a correct SHA-1 hash!");
                        this.getLogger().error("-----------------------------------------------");
                        return;
                    } else {
                        server.sendMessage(Component.text("Hash verification complete for server " + serverName + ".").color(NamedTextColor.GREEN));
                    }
                } catch (Exception e) {
                    this.getLogger().error("Please provide a correct SHA-1 hash/url!");
                    e.printStackTrace();
                }
            }

            resourcePacks.add(new VelocityResourcePack(this, serverName, url, hash, sizeInMB.get()));
        }

        if (!verifyPacks) {
            logger.info("Loaded " + resourcePacks.size() + " resource packs without verification.");
            return;
        }

        server.sendMessage(Component.text("Loaded " + resourcePacks.size() + " verified resource packs.").color(NamedTextColor.GREEN));
    }

    private void checkUnload() {
        final VelocityConfig unloadPack = getConfig().getConfig("unload-pack");
        final boolean enableUnload = unloadPack.getBoolean("enable");
        if (enableUnload) {
            final String url = unloadPack.getString("url");
            final String hash = unloadPack.getString("hash");

            this.checkForRehost(url, "unload-pack");

            final VelocityResourcePack resourcePack = new VelocityResourcePack(this, EMPTY_SERVER_NAME, url, hash, 0);
            resourcePacks.add(resourcePack);
        }
    }

    private void checkGlobal() {
        final VelocityConfig globalPack = getConfig().getConfig("global-pack");
        if (globalPack != null) {
            final boolean enableGlobal = globalPack.getBoolean("enable");
            if (enableGlobal) {
                final String url = globalPack.getString("url");
                final String hash = globalPack.getString("hash");

                this.checkForRehost(url, "global-pack");

                final VelocityResourcePack resourcePack = new VelocityResourcePack(this, GLOBAL_SERVER_NAME, url, hash, 0);
                resourcePacks.add(resourcePack);
                globalResourcePack = resourcePack;
            }
        }
    }

    private void checkValidEnding(String url) {
        List<String> validUrlEndings = Arrays.asList(".zip", ".zip?dl=1");
        boolean hasEnding = false;
        for (String validUrlEnding : validUrlEndings) {
            if (url.endsWith(validUrlEnding)) {
                hasEnding = true;
                break;
            }
        }

        if (!hasEnding) {
            getLogger().error("Your URL has an invalid or unknown format. " +
                    "URLs must have no redirects and use the .zip extension. If you are using Dropbox, change ?dl=0 to ?dl=1.");
            getLogger().error("ForcePack will still load in the event this check is incorrect. Please make an issue or pull request if this is so.");
        }
    }

    private void checkForRehost(String url, String section) {
        List<String> warnForHost = Arrays.asList("convallyria.com");
        boolean rehosted = true;
        for (String host : warnForHost) {
            if (url.contains(host)) {
                rehosted = false;
                break;
            }
        }

        if (!rehosted) {
            getLogger().warn(String.format("[%s] You are using a default resource pack provided by the plugin.", section) +
                    "It's highly recommended you re-host this pack on a CDN such as https://mc-packs.net for faster load times. " +
                    "Leaving this as default potentially sends a lot of requests to my personal web server, which isn't ideal!");
            getLogger().warn("ForcePack will still load and function like normally.");
        }
    }

    @Nullable
    private ResourcePackURLData tryGenerateHash(VelocityConfig resourcePack, String url, String hash, AtomicInteger sizeInMB) {
        if (resourcePack.getBoolean("generate-hash", false)) {
            getLogger().info("Auto-generating ResourcePack hash.");
            getLogger().info("Downloading ResourcePack for hash generation...");
            try {
                ResourcePackURLData data = HashingUtil.performPackCheck(url, hash);
                getLogger().info("Size of ResourcePack: " + data.getSize() + " MB");
                sizeInMB.set(data.getSize());
                getLogger().info("Auto-generated ResourcePack hash: " + hash);
                return data;
            } catch (Exception e) {
                getLogger().error("Unable to auto-generate ResourcePack hash, reverting to config setting", e);
            }
        }
        return null;
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

        if (globalResourcePack != null) {
            return Optional.of(globalResourcePack);
        }

        return Optional.empty();
    }

    public PackHandler getPackHandler() {
        return packHandler;
    }

    private MiniMessage miniMessage;

    public MiniMessage getMiniMessage() {
        if (miniMessage != null) return this.miniMessage;
        return this.miniMessage = MiniMessage.miniMessage();
    }

    public void log(String info, Object... format) {
        if (this.getConfig().getBoolean("debug")) this.getLogger().info(String.format(info, format));
    }
}
