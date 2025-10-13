package com.convallyria.forcepack.sponge;

import com.convallyria.forcepack.api.ForcePackAPI;
import com.convallyria.forcepack.api.ForcePackPlatform;
import com.convallyria.forcepack.api.player.ForcePackPlayer;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.resourcepack.ResourcePackVersion;
import com.convallyria.forcepack.api.schedule.PlatformScheduler;
import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.api.utils.HashingUtil;
import com.convallyria.forcepack.api.verification.ResourcePackURLData;
import com.convallyria.forcepack.sponge.command.Commands;
import com.convallyria.forcepack.sponge.event.MultiVersionResourcePackStatusEvent;
import com.convallyria.forcepack.sponge.listener.ExemptionListener;
import com.convallyria.forcepack.sponge.listener.PacketListener;
import com.convallyria.forcepack.sponge.listener.ResourcePackListener;
import com.convallyria.forcepack.sponge.player.ForcePackSpongePlayer;
import com.convallyria.forcepack.sponge.resourcepack.SpongeResourcePack;
import com.convallyria.forcepack.sponge.schedule.SpongeScheduler;
import com.convallyria.forcepack.sponge.util.FileSystemUtils;
import com.convallyria.forcepack.sponge.util.ProtocolUtil;
import com.convallyria.forcepack.webserver.ForcePackWebServer;
import com.convallyria.forcepack.webserver.downloader.WebServerDependencyDownloader;
import com.github.retrooper.packetevents.PacketEvents;
import com.google.inject.Inject;
import io.github.retrooper.packetevents.sponge.factory.SpongePacketEventsBuilder;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.resource.ResourcePackStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import org.apache.logging.log4j.Logger;
import org.bstats.sponge.Metrics;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Server;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterChannelEvent;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.event.lifecycle.StartingEngineEvent;
import org.spongepowered.api.network.ServerConnectionState;
import org.spongepowered.api.network.channel.raw.RawDataChannel;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Plugin("forcepack")
public class ForcePackSponge implements ForcePackPlatform {

    private final PluginContainer pluginContainer;
    private final Logger logger;
    private final Path configDir;
    private final SpongeScheduler scheduler;

    public final Set<UUID> temporaryExemptedPlayers = new HashSet<>();

    @Override
    public boolean exemptNextResourcePackSend(UUID uuid) {
        return temporaryExemptedPlayers.add(uuid);
    }

    @Inject
    public ForcePackSponge(PluginContainer pluginContainer, Logger logger, @ConfigDir(sharedRoot = false) Path configDir, Metrics.Factory metrics) {
        INSTANCE = this;
        this.pluginContainer = pluginContainer;
        this.logger = logger;
        this.configDir = configDir;
        this.scheduler = new SpongeScheduler(this);
        this.loadConfig();
        metrics.make(13677);
    }

    private final Map<ResourcePackVersion, Set<ResourcePack>> resourcePacks = new HashMap<>();

    @Override
    public Set<ResourcePack> getResourcePacks() {
        return resourcePacks.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Listener
    public void onServerStarting(final StartingEngineEvent<Server> event) {
        registerTranslations();

        PacketEvents.setAPI(SpongePacketEventsBuilder.build(pluginContainer));
        PacketEvents.getAPI().getSettings().debug(debug()).checkForUpdates(false);
        PacketEvents.getAPI().load();

        GeyserUtil.isGeyserInstalledHere = Sponge.pluginManager().plugin("geyser-sponge").isPresent();

        this.registerListeners();
        PacketEvents.getAPI().init();

        // Check server properties
        try {
            this.checkForServerProperties();
        } catch (IOException e) {
            getLogger().error("Failed to check for server properties resource pack", e);
        }

        Runnable run = () -> {
            if (getConfig().node("web-server", "enabled").getBoolean()) {
                try {
                    getLogger().info("Enabling web server...");
                    getLogger().info("Downloading required dependencies, this might take a while! Subsequent startups will be faster.");
                    WebServerDependencyDownloader.download(this, configDir, this::log);
                    getLogger().info("Finished downloading required dependencies.");
                    final String configIp = getConfig().node("web-server", "server-ip").getString("localhost");
                    final String serverIp = !configIp.equals("localhost") ? configIp : ForcePackWebServer.getIp();
                    this.webServer = new ForcePackWebServer(configDir, getConfig().node("web-server", "protocol").getString("http://"), serverIp, getConfig().node("web-server", "port").getInt(8080), getConfig().node("web-server", "port-on-url").getBoolean(true));
                    getLogger().info("Started web server.");
                } catch (IOException e) {
                    getLogger().error("Error starting web server: {}", e.getMessage());
                    getLogger().error("It is highly likely you need to open a port or change it in the config. Please see the config for further information.");
                    return;
                }
            }

            reload();

            this.getLogger().info("Completed loading resource packs.");
        };

        if (getConfig().node("load-last").getBoolean()) {
            scheduler.registerInitTask(run);
        } else {
            run.run();
        }

        if (GeyserUtil.isGeyserInstalledHere && !getConfig().node("Server", "geyser").getBoolean()) {
            getLogger().warn("Geyser is installed but Geyser support is not enabled.");
        } else if (!GeyserUtil.isGeyserInstalledHere && getConfig().node("Server", "geyser").getBoolean()) {
            getLogger().warn("Geyser is not installed but Geyser support is enabled.");
        }
    }

    public Set<ResourcePack> getPacksForVersion(ServerPlayer player) {
        final int protocolVersion = ProtocolUtil.getProtocolVersion(player);
        return getPacksForVersion(protocolVersion);
    }

    private final Map<UUID, ForcePackPlayer> waiting = new HashMap<>();

    public void processWaitingResourcePack(ServerPlayer player, UUID packId) {
        final UUID playerId = player.uniqueId();
        // If the player is on a version older than 1.20.3, they can only have one resource pack.
        if (ProtocolUtil.getProtocolVersion(player) < 765) {
            removeFromWaiting(player);
            return;
        }

        final ForcePackPlayer newPlayer = waiting.computeIfPresent(playerId, (a, forcePackPlayer) -> {
            final Set<ResourcePack> packs = forcePackPlayer.getWaitingPacks();
            packs.removeIf(pack -> pack.getUUID().equals(packId));
            return forcePackPlayer;
        });

        if (newPlayer == null || newPlayer.getWaitingPacks().isEmpty()) {
            removeFromWaiting(player);
        }
    }

    public Optional<ForcePackPlayer> getForcePackPlayer(ServerPlayer player) {
        return Optional.ofNullable(waiting.get(player.uniqueId()));
    }

    public boolean isWaiting(ServerPlayer player) {
        return waiting.containsKey(player.uniqueId());
    }

    public boolean isWaitingFor(ServerPlayer player, UUID packId) {
        if (!isWaiting(player)) return false;

        // If the player is on a version older than 1.20.3, they can only have one resource pack.
        if (ProtocolUtil.getProtocolVersion(player) < 765) {
            return true;
        }

        final Set<ResourcePack> waitingPacks = waiting.get(player.uniqueId()).getWaitingPacks();
        return waitingPacks.stream().anyMatch(pack -> pack.getUUID().equals(packId));
    }

    public void removeFromWaiting(ServerPlayer player) {
        waiting.remove(player.uniqueId());
    }

    public void addToWaiting(UUID uuid, @NonNull Set<ResourcePack> packs) {
        waiting.compute(uuid, (a, existing) -> {
            ForcePackPlayer newPlayer = existing != null ? existing : new ForcePackSpongePlayer(Sponge.server().player(uuid).orElseThrow());
            newPlayer.getWaitingPacks().addAll(packs);
            return newPlayer;
        });
    }

    private @Nullable ForcePackWebServer webServer;

    public Optional<ForcePackWebServer> getWebServer() {
        return Optional.ofNullable(webServer);
    }

    public void reload() {
        if (getConfig().node("velocity-mode").getBoolean()) return;

        resourcePacks.clear(); // Clear for reloads
        getWebServer().ifPresent(ForcePackWebServer::clearHostedPacks);

        final ConfigurationNode packs = getConfig().node("Server", "packs");
        boolean success = true;
        try {
            for (Object key : packs.childrenMap().keySet()) {
                String versionId = key.toString();
                ResourcePackVersion version = getVersionFromId(versionId);
                final ConfigurationNode packSection = packs.node(versionId);
                final List<String> urls = packSection.hasChild("urls")
                        ? packSection.node("urls").getList(String.class, new ArrayList<>())
                        : List.of(packSection.node("url").getString(""));
                final List<String> hashes = packSection.hasChild("hashes")
                        ? packSection.node("hashes").getList(String.class, new ArrayList<>())
                        : List.of(packSection.node("hash").getString(""));

                final boolean generateHash = packSection.node("generate-hash").getBoolean();
                if (!generateHash && urls.size() != hashes.size()) {
                    getLogger().error("There are not the same amount of URLs and hashes! Please provide a hash for every resource pack URL!");
                }

                for (int i = 0; i < urls.size(); i++) {
                    String url = urls.get(i);
                    String hash = i >= hashes.size() ? null : hashes.get(i);
                    success = success && checkPack(version, url, generateHash, hash);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (!success) {
            getLogger().error("Unable to load all resource packs correctly.");
        }
    }

    private boolean checkPack(@Nullable ResourcePackVersion version, String url, boolean generateHash, @Nullable String hash) {
        if (url.startsWith("forcepack://")) { // Localhost
            final File generatedFilePath = new File(configDir + File.separator + url.replace("forcepack://", ""));
            log("Using local resource pack host for " + url + " (" + generatedFilePath + ")");
            if (webServer == null) {
                getLogger().error("Unable to locally host resource pack '{}' because the web server is not active!", url);
                return false;
            }
            webServer.addHostedPack(generatedFilePath);
            url = webServer.getHostedEndpoint(url);
        }

        checkForRehost(url);
        checkValidEnding(url);

        AtomicInteger sizeMB = new AtomicInteger();

        ResourcePackURLData data = null;
        if (generateHash) {
            getLogger().info("Auto-generating resource pack hash.");
            try {
                data = HashingUtil.performPackCheck(url, hash);
                sizeMB.set(data.getSize());
                hash = data.getUrlHash();
                getLogger().info("Size of resource pack: {} MB", sizeMB.get());
                getLogger().info("Auto-generated resource pack hash: {}", hash);
            } catch (Exception e) {
                getLogger().error("Unable to auto-generate resource pack hash, reverting to config setting", e);
            }
        }

        if (getConfig().node("enable-mc-164316-fix").getBoolean()) {
            url = url + "#" + hash;
        }

        if (getConfig().node("Server", "verify").getBoolean()) {
            try {
                Consumer<Integer> consumer = (size) -> {
                    getLogger().info("Performing version size check...");
                    for (ClientVersion clientVersion : ClientVersion.values()) {
                        String sizeStr = clientVersion.getDisplay() + " (" + clientVersion.getMaxSizeMB() + " MB): ";
                        if (clientVersion.getMaxSizeMB() < size) {
                            // Paper support - use console sender for colour
                            Sponge.systemSubject().sendMessage(Component.text(sizeStr + "Unsupported.", NamedTextColor.RED));
                        } else {
                            // Paper support - use console sender for colour
                            Sponge.systemSubject().sendMessage(Component.text(sizeStr + "Supported.", NamedTextColor.GREEN));
                        }
                    }

                    sizeMB.set(size);
                };

                if (data == null) {
                    data = HashingUtil.performPackCheck(url, hash);
                }

                consumer.accept(data.getSize());

                if (hash == null || !hash.equalsIgnoreCase(data.getUrlHash())) {
                    this.getLogger().error("-----------------------------------------------");
                    this.getLogger().error("Your hash does not match the URL file provided!");
                    this.getLogger().error("The URL hash returned: {}", data.getUrlHash());
                    this.getLogger().error("Your config hash returned: {}", data.getConfigHash());
                    this.getLogger().error("Please provide a correct SHA-1 hash!");
                    this.getLogger().error("-----------------------------------------------");
                } else {
                    // Paper support - use console sender for colour
                    Sponge.systemSubject().sendMessage(Component.text("Hash verification complete.", NamedTextColor.GREEN));
                }
            } catch (Exception e) {
                this.getLogger().error("Please provide a correct SHA-1 hash/url!", e);
                return false;
            }
        }

        final String finalUrl = url;
        final String finalHash = hash;
        resourcePacks.compute(version, (u, existingPacks) -> {
            Set<ResourcePack> packs = existingPacks == null ? new HashSet<>() : existingPacks;
            final SpongeResourcePack pack = new SpongeResourcePack(this, finalUrl, finalHash, sizeMB.get(), version);
            packs.add(pack);
            this.getLogger().info("Generated resource pack ({}) for version {} with id {}", pack.getURL(), version == null ? "all" : version, pack.getUUID());
            return packs;
        });
        return true;
    }

    @Listener
    public void onRegisterChannels(RegisterChannelEvent event) {
        if (!getConfig().node("velocity-mode").getBoolean()) return;
        getLogger().info("Enabled velocity listener");
        final RawDataChannel channel = event.register(ResourceKey.of("forcepack", "status"), RawDataChannel.class);
        channel.play().addHandler(ServerConnectionState.Game.class, (message, state) -> {
            final ServerPlayer player = state.player();
            final String data = new String(message.readBytes(message.available()));
            final String[] split = data.split(";");
            log("Posted event");

            final ResourcePackStatus status = ResourcePackStatus.valueOf(split[1]);
            final UUID packId = UUID.fromString(split[0]);
            final boolean proxyRemove = Boolean.parseBoolean(split[2]);
            getScheduler().executeAsync(() -> Sponge.eventManager().post(new MultiVersionResourcePackStatusEvent(player, packId, status, true, proxyRemove)));
        });
    }

    private void registerListeners() {
        EventManager pm = Sponge.eventManager();

        pm.registerListeners(pluginContainer, new ResourcePackListener(this), MethodHandles.lookup());
        pm.registerListeners(pluginContainer, new ExemptionListener(this), MethodHandles.lookup());

        PacketEvents.getAPI().getEventManager().registerListeners(new PacketListener(this));
    }

    @Listener
    private void onRegisterCommands(final RegisterCommandEvent<Command.Parameterized> event) {
        new Commands(this, event.registryHolder());
    }

    private ConfigurationNode rootNode;

    public ConfigurationNode getConfig() {
        return rootNode;
    }

    public void reloadConfig() {
        this.loadConfig();
    }

    private void loadConfig() {
        if (!configDir.toFile().exists()) configDir.toFile().mkdirs();
        final Path configPath = configDir.resolve("config.yml");
        try {
            Files.copy(pluginContainer.openResource("/assets/forcepack/config.yml").orElseThrow(), configPath);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ConfigurationLoader<CommentedConfigurationNode> loader = YamlConfigurationLoader.builder().path(configPath).build();
        try {
            rootNode = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkValidEnding(String url) {
        if (!isValidEnding(url)) {
            getLogger().error("Your URL has an invalid or unknown format. " +
                    "URLs must have no redirects and use the .zip extension. If you are using Dropbox, change dl=0 to dl=1.");
            getLogger().error("ForcePack will still load in the event this check is incorrect. Please make an issue or pull request if this is so.");
        }
    }

    private void checkForRehost(String url) {
        if (isDefaultHost(url)) {
            getLogger().warn(String.format("[%s] You are using a default resource pack provided by the plugin. ", url) +
                    " It's highly recommended you re-host this pack using the webserver or on a CDN such as https://mc-packs.net for faster load times. " +
                    "Leaving this as default potentially sends a lot of requests to my personal web server, which isn't ideal!");
            getLogger().warn("ForcePack will still load and function like normally.");
        }

        getBlacklistedSite(url).ifPresent(blacklistedSite -> {
            getLogger().error("Invalid resource pack site used! '{}' cannot be used for hosting resource packs!", blacklistedSite);
        });
    }

    private void checkForServerProperties() throws IOException {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader("./server.properties")) {
            properties.load(reader);
            String packUrl = properties.getProperty("resource-pack");
            if (packUrl != null && !packUrl.isEmpty()) {
                getLogger().error("You have a resource pack set in server.properties!");
                getLogger().error("This will cause ForcePack to not function correctly. You MUST remove the resource pack URL from server.properties!");
            }
        }
    }


    private void registerTranslations() {
        final TranslationRegistry translationRegistry = TranslationRegistry.create(Key.key("forcepack", "translations"));
        translationRegistry.defaultLocale(Locale.US);

        try {
            FileSystemUtils.visitResources(ForcePackSponge.class, path -> {
                this.getLogger().info("Loading localizations...");

                try (final Stream<Path> stream = Files.walk(path)) {
                    stream.forEach(file -> {
                        if (!Files.isRegularFile(file)) {
                            return;
                        }

                        final String filename = com.google.common.io.Files.getNameWithoutExtension(file.getFileName().toString());
                        final String localeName = filename
                                .replace("messages_", "")
                                .replace("messages", "")
                                .replace('_', '-');
                        final Locale locale = localeName.isEmpty() ? Locale.US : Locale.forLanguageTag(localeName);

                        translationRegistry.registerAll(locale, ResourceBundle.getBundle("com/convallyria/forcepack/sponge/l10n/messages",
                                locale), false);

                        this.getLogger().info("Loaded translations for {}.", locale.getDisplayName());
                    });
                } catch (final IOException e) {
                    getLogger().warn("Encountered an I/O error whilst loading translations", e);
                }
            }, "com", "convallyria", "forcepack", "sponge", "l10n");
        } catch (final IOException e) {
            getLogger().warn("Encountered an I/O error whilst loading translations", e);
            return;
        }

        GlobalTranslator.translator().addSource(translationRegistry);
    }

    public PluginContainer pluginContainer() {
        return pluginContainer;
    }

    @Override
    public PlatformScheduler<?> getScheduler() {
        return scheduler;
    }

    public boolean debug() {
        return getConfig().node("Server", "debug").getBoolean();
    }

    @Override
    public void log(String info, Object... format) {
        if (debug()) getLogger().info(String.format(info, format));
    }

    public Logger getLogger() {
        return logger;
    }

    private static ForcePackSponge INSTANCE;

    public static ForcePackAPI getAPI() {
        return getInstance();
    }

    public static ForcePackSponge getInstance() {
        return INSTANCE;
    }
}
