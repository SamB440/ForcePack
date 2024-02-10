package com.convallyria.forcepack.velocity;

import com.convallyria.forcepack.api.ForcePackAPI;
import com.convallyria.forcepack.api.resourcepack.PackFormatResolver;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.resourcepack.ResourcePackVersion;
import com.convallyria.forcepack.api.schedule.PlatformScheduler;
import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.api.utils.HashingUtil;
import com.convallyria.forcepack.api.verification.ResourcePackURLData;
import com.convallyria.forcepack.velocity.command.Commands;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.convallyria.forcepack.velocity.handler.PackHandler;
import com.convallyria.forcepack.velocity.listener.ResourcePackListener;
import com.convallyria.forcepack.velocity.resourcepack.VelocityResourcePack;
import com.convallyria.forcepack.velocity.schedule.VelocityScheduler;
import com.convallyria.forcepack.webserver.ForcePackWebServer;
import com.convallyria.forcepack.webserver.downloader.WebServerDependencyDownloader;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Plugin(
        id = "forcepack",
        name = "ForcePack",
        version = "1.3.3",
        description = "Force players to use your server resource pack.",
        url = "https://www.convallyria.com",
        dependencies = {
            @Dependency(id = "viaversion", optional = true),
            @Dependency(id = "viabackwards", optional = true),
            @Dependency(id = "viarewind", optional = true)
        },
        authors = {"SamB440"}
)
public class ForcePackVelocity implements ForcePackAPI {

    public static final String EMPTY_SERVER_NAME = "ForcePack-Empty-Server";
    public static final String GLOBAL_SERVER_NAME = "ForcePack-Global-Server";

    private final ProxyServer server;
    private final Logger logger;
    private final Commands commands;
    private final Path dataDirectory;
    private final Metrics.Factory metricsFactory;
    private final CommandManager commandManager;
    private final VelocityScheduler scheduler;

    private ForcePackWebServer webServer;

    public Optional<ForcePackWebServer> getWebServer() {
        return Optional.ofNullable(webServer);
    }

    @Inject
    public ForcePackVelocity(PluginContainer container, ProxyServer server, Logger logger, @DataDirectory Path dataDirectory, Metrics.Factory metricsFactory, CommandManager commandManager) {
        this.server = server;
        this.logger = logger;
        this.commands = new Commands(this, container);
        this.dataDirectory = dataDirectory;
        this.metricsFactory = metricsFactory;
        this.commandManager = commandManager;
        this.scheduler = new VelocityScheduler(this);
    }

    private VelocityConfig config;
    private PackHandler packHandler;
    private final Set<ResourcePack> globalResourcePacks = new HashSet<>();
    private final Set<ResourcePack> resourcePacks = new HashSet<>();

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        getLogger().info("Enabling ForcePack (velocity)...");
        GeyserUtil.isGeyserInstalledHere = server.getPluginManager().getPlugin("geyser").isPresent();
        this.reloadConfig();

        final VelocityConfig webServerConfig = getConfig().getConfig("web-server");
        if (webServerConfig != null && webServerConfig.getBoolean("enabled")) {
            try {
                getLogger().info("Enabling web server...");
                getLogger().info("Downloading required dependencies, this might take a while! Subsequent startups will be faster.");
                WebServerDependencyDownloader.download(this, getDataDirectory(), this::log);
                getLogger().info("Finished downloading required dependencies.");
                final String configIp = webServerConfig.getString("server-ip", "localhost");
                final String serverIp = !configIp.equals("localhost") ? configIp : ForcePackWebServer.getIp();
                this.webServer = new ForcePackWebServer(dataDirectory, serverIp, webServerConfig.getInt("port", 8080));
                getLogger().info("Started web server.");
            } catch (IOException e) {
                getLogger().error("Error starting web server: " + e.getMessage());
                getLogger().error("It is highly likely you need to open a port or change it in the config. Please see the config for further information.");
                return;
            }
        }

        this.packHandler = new PackHandler(this);
        this.loadResourcePacks(null);
        this.registerListeners();
        metricsFactory.make(this, 13678);
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        if (webServer != null) webServer.shutdown();
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
    }

    public void reloadConfig() {
        this.createConfig();
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
                if (packHandler.isWaiting(player)) {
                    log("Stopping command '%s' because player has not loaded all their resource packs yet.", command);
                    event.setResult(CommandExecuteEvent.CommandResult.denied());
                }
            }
        });
    }

    public void loadResourcePacks(@Nullable Player player) {
        // Clear for reloads
        resourcePacks.clear();
        globalResourcePacks.clear();

        getWebServer().ifPresent(ForcePackWebServer::clearHostedPacks);

        this.checkUnload();
        this.checkGlobal();

        final boolean verifyPacks = getConfig().getBoolean("verify-resource-packs");
        final VelocityConfig groups = getConfig().getConfig("groups");
        if (groups != null) {
            addResourcePacks(player, "groups");
        }

        addResourcePacks(player, "servers");

        if (!verifyPacks) {
            logger.info("Loaded " + resourcePacks.size() + " resource packs without verification.");
            return;
        }

        final ConsoleCommandSource consoleSender = this.getServer().getConsoleCommandSource();
        Component loadedMsg = Component.text("Loaded " + resourcePacks.size() + " verified resource packs.").color(NamedTextColor.GREEN);
        consoleSender.sendMessage(loadedMsg);
        if (player != null) player.sendMessage(loadedMsg);
    }

    private void addResourcePacks(@Nullable Player player, String rootName) {
        final boolean verifyPacks = getConfig().getBoolean("verify-resource-packs");
        final boolean groups = rootName.equals("groups");
        final String typeName = groups ? "group" : "server";
        VelocityConfig root = groups ? getConfig().getConfig("groups") : getConfig().getConfig("servers");
        for (String name : root.getKeys()) {
            log("Checking %s - %s", typeName, name);
            final VelocityConfig serverConfig = root.getConfig(name);
            final Map<String, VelocityConfig> configs = new HashMap<>();
            // Add the default fallback
            configs.put("default", serverConfig.getConfig("resourcepack"));
            final VelocityConfig versionConfig = serverConfig.getConfig("version");
            if (versionConfig != null) {
                log("Detected versioned resource packs for %s", name);
                for (String versionId : versionConfig.getKeys()) {
                    configs.put(versionId, versionConfig.getConfig(versionId).getConfig("resourcepack"));
                    log("Added version config %s for %s", versionId, name);
                }
            }

            configs.forEach((id, config) -> {
                if (config == null) {
                    log("Invalid resource pack config found for %s. You probably forgot to rename something!", id);
                    return;
                }

                this.registerResourcePack(serverConfig, config, id, name, typeName, groups, verifyPacks, player);
            });
        }
    }
    
    private void registerResourcePack(VelocityConfig rootServerConfig, VelocityConfig resourcePack, String id, String name, String typeName, boolean groups, boolean verifyPacks, @Nullable Player player) {
        List<String> urls = resourcePack.getStringList("urls");
        if (urls.isEmpty()) {
           urls = List.of(resourcePack.getString("url", ""));
        }

        List<String> hashes = resourcePack.getStringList("hashes");
        if (hashes.isEmpty()) {
            hashes = List.of(resourcePack.getString("hash", ""));
        }

        final boolean generateHash = resourcePack.getBoolean("generate-hash", false);
        if (!generateHash && urls.size() != hashes.size()) {
            getLogger().error("There are not the same amount of URLs and hashes! Please provide a hash for every resource pack URL! (" + id + ", " + name + ")");
            getLogger().error("Hint: Enabling generate-hash will auto-generate missing hashes");
        }

        for (int i = 0; i < urls.size(); i++) {
            final String url = urls.get(i);
            final String hash = i >= hashes.size() ? null : hashes.get(i);
            this.handleRegister(rootServerConfig, resourcePack, name, id, typeName, url, hash, groups, verifyPacks, player);
        }
    }

    private void handleRegister(VelocityConfig rootServerConfig, VelocityConfig resourcePack, String name, String id, String typeName, String url, @Nullable String hash, boolean groups, boolean verifyPacks, @Nullable Player player) {
        final ConsoleCommandSource consoleSender = this.getServer().getConsoleCommandSource();
        if (url.isEmpty()) {
            logger.error("No URL found for " + name + ". Did you set up the config correctly?");
        }

        AtomicInteger sizeInMB = new AtomicInteger();

        url = this.checkLocalHostUrl(url);
        this.checkValidEnding(url);
        this.checkForRehost(url, name);

        ResourcePackURLData data = this.tryGenerateHash(resourcePack, url, hash, sizeInMB);
        if (data != null) hash = data.getUrlHash();

        if (getConfig().getBoolean("enable-mc-164316-fix", false)) {
            url = url + "#" + hash;
        }

        if (verifyPacks) {
            try {
                Consumer<Integer> consumer = (size) -> {
                    getLogger().info("Performing version size check for " + name + " (" + id + ")...");
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

                if (hash == null || !hash.equalsIgnoreCase(data.getUrlHash())) {
                    this.getLogger().error("-----------------------------------------------");
                    this.getLogger().error("Your hash does not match the URL file provided!");
                    this.getLogger().error("Target " + typeName + ": " + name + " (" + id + ")");
                    this.getLogger().error("The URL hash returned: " + data.getUrlHash());
                    this.getLogger().error("Your config hash returned: " + data.getConfigHash());
                    this.getLogger().error("Please provide a correct SHA-1 hash!");
                    this.getLogger().error("-----------------------------------------------");
                    return;
                } else {
                    Component hashMsg = Component.text("Hash verification complete for " + typeName + " " + name + " (" + id + ").").color(NamedTextColor.GREEN);
                    consoleSender.sendMessage(hashMsg);
                    if (player != null) player.sendMessage(hashMsg);
                }
            } catch (Exception e) {
                this.getLogger().error("Please provide a correct SHA-1 hash/url!", e);
            }
        }

        ResourcePackVersion version = null;
        try {
            final int versionId = Integer.parseInt(id);
            version = () -> versionId;
        } catch (NumberFormatException ignored) {}

        if (groups) {
            final boolean exact = rootServerConfig.getBoolean("exact-match");
            for (String serverName : rootServerConfig.getStringList("servers")) {
                for (RegisteredServer registeredServer : server.getAllServers()) {
                    final String serverInfoName = registeredServer.getServerInfo().getName();
                    final boolean matches = exact ?
                            serverInfoName.equals(serverName) :
                            serverInfoName.contains(serverName);
                    if (!matches) continue;
                    final VelocityResourcePack pack = new VelocityResourcePack(this, serverInfoName, url, hash, sizeInMB.get(), name, version);
                    resourcePacks.add(pack);
                    log("Added resource pack for server %s (%s)", serverInfoName, pack.getUUID().toString());
                }
            }
        } else {
            resourcePacks.add(new VelocityResourcePack(this, name, url, hash, sizeInMB.get(), null, version));
        }
    }

    private void checkUnload() {
        final VelocityConfig unloadPack = getConfig().getConfig("unload-pack");
        final boolean enableUnload = unloadPack.getBoolean("enable");
        if (enableUnload) {
            String url = unloadPack.getString("url", "");

            url = this.checkLocalHostUrl(url);
            this.checkValidEnding(url);
            this.checkForRehost(url, "unload-pack");

            String hash = unloadPack.getString("hash");

            ResourcePackURLData data = this.tryGenerateHash(unloadPack, url, hash, new AtomicInteger(0));
            if (data != null) hash = data.getUrlHash();

            final VelocityResourcePack resourcePack = new VelocityResourcePack(this, EMPTY_SERVER_NAME, url, hash, 0, null, null);
            resourcePacks.add(resourcePack);
        }
    }

    private void checkGlobal() {
        final VelocityConfig globalPack = getConfig().getConfig("global-pack");
        if (globalPack == null) return;

        final boolean enableGlobal = globalPack.getBoolean("enable");
        if (!enableGlobal) return;

        final Map<String, VelocityConfig> configs = new HashMap<>();
        // Add the default fallback
        configs.put("default", globalPack);
        final VelocityConfig versionConfig = globalPack.getConfig("version");
        if (versionConfig != null) {
            log("Detected versioned resource packs for global pack");
            for (String versionId : versionConfig.getKeys()) {
                configs.put(versionId, versionConfig.getConfig(versionId));
                log("Added version config %s for global pack", versionId);
            }
        }

        configs.forEach((id, config) -> {
            List<String> urls = config.getStringList("urls");
            if (urls.isEmpty()) {
                urls = List.of(config.getString("url", ""));
            }

            List<String> hashes = config.getStringList("hashes");
            if (hashes.isEmpty()) {
                hashes = List.of(config.getString("hash", ""));
            }

            final boolean generateHash = config.getBoolean("generate-hash", false);
            if (!generateHash && urls.size() != hashes.size()) {
                getLogger().error("[global-pack] There are not the same amount of URLs and hashes! Please provide a hash for every resource pack URL! (" + id + ")");
                getLogger().error("Hint: Enabling generate-hash will auto-generate missing hashes");
            }

            for (int i = 0; i < urls.size(); i++) {
                final String url = urls.get(i);
                final String hash = i >= hashes.size() ? null : hashes.get(i);
                this.registerGlobalResourcePack(config, id, url, hash);
            }
        });
    }

    private void registerGlobalResourcePack(VelocityConfig globalPack, String id, String url, String hash) {
        url = this.checkLocalHostUrl(url);
        this.checkValidEnding(url);
        this.checkForRehost(url, "global-pack");

        ResourcePackURLData data = this.tryGenerateHash(globalPack, url, hash, new AtomicInteger(0));
        if (data != null) hash = data.getUrlHash();

        if (getConfig().getBoolean("enable-mc-164316-fix", false)) {
            url = url + "#" + hash;
        }

        ResourcePackVersion version = null;
        try {
            final int versionId = Integer.parseInt(id);
            version = () -> versionId;
        } catch (NumberFormatException ignored) {}

        final VelocityResourcePack resourcePack = new VelocityResourcePack(this, GLOBAL_SERVER_NAME + "-" + url, url, hash, 0, null, version);
        resourcePacks.add(resourcePack);
        globalResourcePacks.add(resourcePack);
    }

    private String checkLocalHostUrl(String url) {
        if (url.startsWith("forcepack://")) { // Localhost
            final File generatedFilePath = new File(getDataDirectory() + File.separator + url.replace("forcepack://", ""));
            log("Using local resource pack host for " + url + " (" + generatedFilePath + ")");
            if (getWebServer().isEmpty()) {
                getLogger().error("Unable to locally host resource pack '" + url + "' because the web server is not active!");
                return url;
            }
            webServer.addHostedPack(generatedFilePath);
            url = webServer.getHostedEndpoint(url);
        }
        return url;
    }

    private void checkValidEnding(String url) {
        List<String> validUrlEndings = Arrays.asList(".zip", "dl=1");
        boolean hasEnding = false;
        for (String validUrlEnding : validUrlEndings) {
            if (url.endsWith(validUrlEnding)) {
                hasEnding = true;
                break;
            }
        }

        if (!hasEnding) {
            getLogger().error("Your URL has an invalid or unknown format. " +
                    "URLs must have no redirects and use the .zip extension. If you are using Dropbox, change dl=0 to dl=1.");
            getLogger().error("ForcePack will still load in the event this check is incorrect. Please make an issue or pull request if this is so.");
        }
    }

    private void checkForRehost(String url, String section) {
        List<String> warnForHost = List.of("convallyria.com");
        boolean rehosted = true;
        for (String host : warnForHost) {
            if (url.contains(host)) {
                rehosted = false;
                break;
            }
        }

        if (!rehosted) {
            getLogger().warn(String.format("[%s] You are using a default resource pack provided by the plugin. ", section) +
                    " It's highly recommended you re-host this pack using the webserver or on a CDN such as https://mc-packs.net for faster load times. " +
                    "Leaving this as default potentially sends a lot of requests to my personal web server, which isn't ideal!");
            getLogger().warn("ForcePack will still load and function like normally.");
        }

        List<String> blacklisted = List.of("mediafire.com");
        for (String blacklistedSite : blacklisted) {
            if (url.contains(blacklistedSite)) {
                getLogger().error("Invalid resource pack site used! '" + blacklistedSite + "' cannot be used for hosting resource packs!");
            }
        }
    }

    @Nullable
    private ResourcePackURLData tryGenerateHash(VelocityConfig resourcePack, String url, String hash, AtomicInteger sizeInMB) {
        if (resourcePack.getBoolean("generate-hash", false)) {
            getLogger().info("Auto-generating resource pack hash.");
            getLogger().info("Downloading resource pack for hash generation...");
            try {
                ResourcePackURLData data = HashingUtil.performPackCheck(url, hash);
                getLogger().info("Size of resource pack: " + data.getSize() + " MB");
                sizeInMB.set(data.getSize());
                getLogger().info("Auto-generated resource pack hash: " + data.getUrlHash());
                return data;
            } catch (Exception e) {
                getLogger().error("Unable to auto-generate resource pack hash, reverting to config setting", e);
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
    public Set<ResourcePack> getResourcePacks() {
        return Collections.unmodifiableSet(resourcePacks);
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler;
    }

    public Optional<Set<ResourcePack>> getPacksByServerAndVersion(final String server, final ProtocolVersion version) {
        final int protocolVersion = version.getProtocol();
        final int packFormat = PackFormatResolver.getPackFormat(protocolVersion);
        return searchForValidPacks(resourcePacks, server, version, packFormat).or(() -> searchForValidPacks(globalResourcePacks, GLOBAL_SERVER_NAME, version, packFormat));
    }

    private Optional<Set<ResourcePack>> searchForValidPacks(Set<ResourcePack> packs, String serverName, final ProtocolVersion protocolVersion, int packVersion) {
        Set<ResourcePack> validPacks = new HashSet<>();
        ResourcePack anyVersionPack = null;
        for (ResourcePack resourcePack : packs.stream().filter(pack -> {
            boolean matches = pack.getServer().equals(serverName) || (serverName.equals(GLOBAL_SERVER_NAME) && pack.getServer().contains(GLOBAL_SERVER_NAME));
            if (!matches) log("Filtering out %s: %s != %s", pack.getUUID().toString(), pack.getServer(), serverName);
            return matches;
        }).collect(Collectors.toList())) {
            log("Trying resource pack %s (%s)", resourcePack.getURL(), resourcePack.getVersion().map(ResourcePackVersion::version).toString());

            final Optional<ResourcePackVersion> version = resourcePack.getVersion();
            if (version.isEmpty()) {
                if (anyVersionPack == null) anyVersionPack = resourcePack; // Pick first all-version resource pack
                validPacks.add(resourcePack); // This is still a valid pack that we want to apply.
                continue;
            }

            if (version.get().version() == packVersion) {
                validPacks.add(resourcePack);
                if (protocolVersion.getProtocol() < 765) { // If < 1.20.3, only one pack can be applied.
                    break;
                }
            }
        }

        if (!validPacks.isEmpty()) {
            return Optional.of(validPacks);
        }

        return anyVersionPack == null ? Optional.empty() : Optional.of(Set.of(anyVersionPack));
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
