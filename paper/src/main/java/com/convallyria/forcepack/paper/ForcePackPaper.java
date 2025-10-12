package com.convallyria.forcepack.paper;

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
import com.convallyria.forcepack.folia.schedule.FoliaScheduler;
import com.convallyria.forcepack.paper.command.Commands;
import com.convallyria.forcepack.paper.integration.ItemsAdderIntegration;
import com.convallyria.forcepack.paper.listener.ExemptionListener;
import com.convallyria.forcepack.paper.listener.PacketListener;
import com.convallyria.forcepack.paper.listener.ResourcePackListener;
import com.convallyria.forcepack.paper.listener.VelocityMessageListener;
import com.convallyria.forcepack.paper.player.ForcePackPaperPlayer;
import com.convallyria.forcepack.paper.resourcepack.PaperResourcePack;
import com.convallyria.forcepack.paper.schedule.BukkitScheduler;
import com.convallyria.forcepack.paper.translation.Translations;
import com.convallyria.forcepack.paper.util.ProtocolUtil;
import com.convallyria.forcepack.webserver.ForcePackWebServer;
import com.convallyria.forcepack.webserver.downloader.WebServerDependencyDownloader;
import com.convallyria.languagy.api.adventure.AdventurePlatform;
import com.convallyria.languagy.api.language.Language;
import com.convallyria.languagy.api.language.Translator;
import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class ForcePackPaper extends JavaPlugin implements ForcePackPlatform {

    private Translator translator;
    private PlatformScheduler<?> scheduler;
    private final Map<ResourcePackVersion, Set<ResourcePack>> resourcePacks = new HashMap<>();
    public boolean velocityMode;

    private BukkitAudiences adventure;

    public final Set<UUID> temporaryExemptedPlayers = new HashSet<>();

    @Override
    public Set<ResourcePack> getResourcePacks() {
        return resourcePacks.values().stream()
                .flatMap(Set::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<ResourcePack> getPacksForVersion(Player player) {
        final int protocolVersion = ProtocolUtil.getProtocolVersion(player);
        return getPacksForVersion(protocolVersion);
    }

    @Override
    public PlatformScheduler<?> getScheduler() {
        return scheduler;
    }

    @Override
    public boolean exemptNextResourcePackSend(UUID uuid) {
        return temporaryExemptedPlayers.add(uuid);
    }

    private final Map<UUID, ForcePackPlayer> waiting = new HashMap<>();

    public void processWaitingResourcePack(Player player, UUID packId) {
        final UUID playerId = player.getUniqueId();
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

    public Optional<ForcePackPlayer> getForcePackPlayer(Player player) {
        return Optional.ofNullable(waiting.get(player.getUniqueId()));
    }

    public boolean isWaiting(Player player) {
        return waiting.containsKey(player.getUniqueId());
    }

    public boolean isWaitingFor(Player player, UUID packId) {
        if (!isWaiting(player)) return false;

        // If the player is on a version older than 1.20.3, they can only have one resource pack.
        if (ProtocolUtil.getProtocolVersion(player) < 765) {
            return true;
        }

        final Set<ResourcePack> waitingPacks = waiting.get(player.getUniqueId()).getWaitingPacks();
        return waitingPacks.stream().anyMatch(pack -> pack.getUUID().equals(packId));
    }

    public void removeFromWaiting(Player player) {
        waiting.remove(player.getUniqueId());
    }

    public void addToWaiting(UUID uuid, @NonNull Set<ResourcePack> packs) {
        waiting.compute(uuid, (a, existing) -> {
            ForcePackPlayer newPlayer = existing != null ? existing : new ForcePackPaperPlayer(Bukkit.getPlayer(uuid));
            newPlayer.getWaitingPacks().addAll(packs);
            return newPlayer;
        });
    }

    private @Nullable ForcePackWebServer webServer;

    public Optional<ForcePackWebServer> getWebServer() {
        return Optional.ofNullable(webServer);
    }

    @Override
    public void onEnable() {
        this.generateLang();
        this.createConfig();

        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().debug(debug()).preViaInjection(true).checkForUpdates(false);
        PacketEvents.getAPI().load();

        GeyserUtil.isGeyserInstalledHere = Bukkit.getPluginManager().getPlugin("Geyser-Spigot") != null;

        this.adventure = BukkitAudiences.create(this);
        MiniMessage miniMessage = MiniMessage.miniMessage();
        this.velocityMode = getConfig().getBoolean("velocity-mode");
        this.scheduler = FoliaScheduler.RUNNING_FOLIA ? new FoliaScheduler(this) : new BukkitScheduler(this);
        this.registerListeners();
        this.registerCommands();
        this.translator = Translator.of(this, "lang", Language.BRITISH_ENGLISH, debug(), AdventurePlatform.create(miniMessage, adventure));
        PacketEvents.getAPI().init();

        // Convert legacy config
        // Check server properties, too
        try {
            this.performLegacyCheck();
            this.checkForServerProperties();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Runnable run = () -> {
            if (getConfig().getBoolean("web-server.enabled")) {
                try {
                    getLogger().info("Enabling web server...");
                    getLogger().info("Downloading required dependencies, this might take a while! Subsequent startups will be faster.");
                    WebServerDependencyDownloader.download(this, getDataFolder().toPath(), this::log);
                    getLogger().info("Finished downloading required dependencies.");
                    final String configIp = getConfig().getString("web-server.server-ip", "localhost");
                    final String serverIp = !configIp.equals("localhost") ? configIp : !Bukkit.getIp().isEmpty() ? Bukkit.getIp() : ForcePackWebServer.getIp();
                    this.webServer = new ForcePackWebServer(this.getDataFolder().toPath(), getConfig().getString("protocol", "http://"), serverIp, getConfig().getInt("port", 8080), getConfig().getBoolean("port-on-url", true));
                    getLogger().info("Started web server.");
                } catch (IOException e) {
                    getLogger().severe("Error starting web server: " + e.getMessage());
                    getLogger().severe("It is highly likely you need to open a port or change it in the config. Please see the config for further information.");
                    return;
                }
            }

            reload();

            new Metrics(this, 13677);

            this.getLogger().info("Completed loading resource packs.");
        };

        if (getConfig().getBoolean("await-items-adder-host") && Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
            new ItemsAdderIntegration(this, run);
            return;
        }

        if (getConfig().getBoolean("load-last")) {
            scheduler.registerInitTask(run);
        } else {
            run.run();
        }

        if (GeyserUtil.isGeyserInstalledHere && !getConfig().getBoolean("Server.geyser")) {
            getLogger().warning("Geyser is installed but Geyser support is not enabled.");
        } else if (!GeyserUtil.isGeyserInstalledHere && getConfig().getBoolean("Server.geyser")) {
            getLogger().warning("Geyser is not installed but Geyser support is enabled.");
        }
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
        if (webServer != null) webServer.shutdown();
        if (translator != null) translator.close();
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

    public void reload() {
        if (velocityMode) return;

        resourcePacks.clear(); // Clear for reloads
        getWebServer().ifPresent(ForcePackWebServer::clearHostedPacks);

        final ConfigurationSection packs = getConfig().getConfigurationSection("Server.packs");
        boolean success = true;
        for (String versionId : packs.getKeys(false)) {
            ResourcePackVersion version = getVersionFromId(versionId);
            final ConfigurationSection packSection = packs.getConfigurationSection(versionId);
            final List<String> urls = packSection.isSet("urls")
                    ? packSection.getStringList("urls")
                    : List.of(packSection.getString("url", ""));
            final List<String> hashes = packSection.isSet("hashes")
                    ? packSection.getStringList("hashes")
                    : List.of(packSection.getString("hash", ""));

            final boolean generateHash = packSection.getBoolean("generate-hash");
            if (!generateHash && urls.size() != hashes.size()) {
                getLogger().severe("There are not the same amount of URLs and hashes! Please provide a hash for every resource pack URL!");
            }

            for (int i = 0; i < urls.size(); i++) {
                String url = urls.get(i);
                String hash = i >= hashes.size() ? null : hashes.get(i);
                success = success && checkPack(version, url, generateHash, hash);
            }
        }

        if (!success) {
            getLogger().severe("Unable to load all resource packs correctly.");
        }
    }

    private boolean checkPack(@Nullable ResourcePackVersion version, String url, boolean generateHash, @Nullable String hash) {
        if (url.startsWith("forcepack://")) { // Localhost
            final File generatedFilePath = new File(getDataFolder() + File.separator + url.replace("forcepack://", ""));
            log("Using local resource pack host for " + url + " (" + generatedFilePath + ")");
            if (webServer == null) {
                getLogger().severe("Unable to locally host resource pack '" + url + "' because the web server is not active!");
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
                getLogger().info("Size of resource pack: " + sizeMB.get() + " MB");
                getLogger().info("Auto-generated resource pack hash: " + hash);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Unable to auto-generate resource pack hash, reverting to config setting", e);
            }
        }

        if (getConfig().getBoolean("enable-mc-164316-fix")) {
            url = url + "#" + hash;
        }

        if (getConfig().getBoolean("Server.verify")) {
            try {
                Consumer<Integer> consumer = (size) -> {
                    getLogger().info("Performing version size check...");
                    for (ClientVersion clientVersion : ClientVersion.values()) {
                        String sizeStr = clientVersion.getDisplay() + " (" + clientVersion.getMaxSizeMB() + " MB): ";
                        if (clientVersion.getMaxSizeMB() < size) {
                            // Paper support - use console sender for colour
                            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + sizeStr + "Unsupported.");
                        } else {
                            // Paper support - use console sender for colour
                            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + sizeStr + "Supported.");
                        }
                    }

                    sizeMB.set(size);
                };

                if (data == null) {
                    data = HashingUtil.performPackCheck(url, hash);
                }

                consumer.accept(data.getSize());

                if (hash == null || !hash.equalsIgnoreCase(data.getUrlHash())) {
                    this.getLogger().severe("-----------------------------------------------");
                    this.getLogger().severe("Your hash does not match the URL file provided!");
                    this.getLogger().severe("The URL hash returned: " + data.getUrlHash());
                    this.getLogger().severe("Your config hash returned: " + data.getConfigHash());
                    this.getLogger().severe("Please provide a correct SHA-1 hash!");
                    this.getLogger().severe("-----------------------------------------------");
                } else {
                    // Paper support - use console sender for colour
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Hash verification complete.");
                }
            } catch (Exception e) {
                this.getLogger().severe("Please provide a correct SHA-1 hash/url!");
                e.printStackTrace();
                Bukkit.getPluginManager().disablePlugin(this);
                return false;
            }
        }

        final String finalUrl = url;
        final String finalHash = hash;
        resourcePacks.compute(version, (u, existingPacks) -> {
            Set<ResourcePack> packs = existingPacks == null ? new HashSet<>() : existingPacks;
            final PaperResourcePack pack = new PaperResourcePack(this, finalUrl, finalHash, sizeMB.get(), version);
            packs.add(pack);
            this.getLogger().info("Generated resource pack (" + pack.getURL() + ") for version " + (version == null ? "all" : version) + " with id " + pack.getUUID());
            return packs;
        });
        return true;
    }

    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        if (velocityMode) {
            getLogger().info("Enabled velocity listener");
            this.getServer().getMessenger().registerIncomingPluginChannel(this, "forcepack:status", new VelocityMessageListener(this));
        }

        pm.registerEvents(new ResourcePackListener(this), this);
        pm.registerEvents(new ExemptionListener(this), this);

        PacketEvents.getAPI().getEventManager().registerListeners(new PacketListener(this));
    }

    private void registerCommands() {
        new Commands(this);
    }

    private void generateLang() {
        Translations.generateLang(this);
    }

    private void createConfig() {
        saveDefaultConfig();
    }

    private void checkValidEnding(String url) {
        if (!isValidEnding(url)) {
            getLogger().severe("Your URL has an invalid or unknown format. " +
                    "URLs must have no redirects and use the .zip extension. If you are using Dropbox, change dl=0 to dl=1.");
            getLogger().severe("ForcePack will still load in the event this check is incorrect. Please make an issue or pull request if this is so.");
        }
    }

    private void checkForRehost(String url) {
        if (isDefaultHost(url)) {
            getLogger().warning(String.format("[%s] You are using a default resource pack provided by the plugin. ", url) +
                    " It's highly recommended you re-host this pack using the webserver or on a CDN such as https://mc-packs.net for faster load times. " +
                    "Leaving this as default potentially sends a lot of requests to my personal web server, which isn't ideal!");
            getLogger().warning("ForcePack will still load and function like normally.");
        }

        getBlacklistedSite(url).ifPresent(blacklistedSite -> {
            getLogger().severe("Invalid resource pack site used! '" + blacklistedSite + "' cannot be used for hosting resource packs!");
        });
    }

    private void performLegacyCheck() throws IOException {
        final Map<String, PlayerResourcePackStatusEvent.Status> sections = Map.of(
                "Server.Actions.On_Accept", PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED,
                "Server.Actions.On_Deny", PlayerResourcePackStatusEvent.Status.DECLINED,
                "Server.Actions.On_Fail", PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD);
        final boolean kick = getConfig().getBoolean("Server.kick");
        for (String sectionName : sections.keySet()) {
            final PlayerResourcePackStatusEvent.Status status = sections.get(sectionName);
            final ConfigurationSection section = getConfig().getConfigurationSection(sectionName);
            if (section != null) {
                getLogger().warning("Detected legacy '" + sectionName + "' action, converting your config now (consider regenerating config for comments and new settings!)...");
                getConfig().set("Server.Actions." + status.name() + ".Commands", section.getStringList("Command"));
                getConfig().set("Server.Actions." + status.name() + ".kick", status != PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED && kick);
                getConfig().set(sectionName, null);
                getConfig().set("Server.kick", null);
                getConfig().save(new File(getDataFolder() + File.separator + "config.yml"));
            }
        }

        final ConfigurationSection oldPackSection = getConfig().getConfigurationSection("Server.ResourcePack");
        if (oldPackSection != null) {
            getLogger().warning("Detected legacy resource pack section, converting your config now (consider regenerating config for comments and new settings!)...");
            final String oldUrl = oldPackSection.getString("url");
            final boolean oldGenerateHash = oldPackSection.getBoolean("generate-hash");
            final String oldHash = oldPackSection.getString("hash");
            getConfig().set("Server.packs.all.url", oldUrl);
            getConfig().set("Server.packs.all.generate-hash", oldGenerateHash);
            getConfig().set("Server.packs.all.hash", oldHash);
            getConfig().set("Server.ResourcePack", null);
            getConfig().save(new File(getDataFolder() + File.separator + "config.yml"));
        }
    }

    private void checkForServerProperties() throws IOException {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader("./server.properties")) {
            properties.load(reader);
            String packUrl = properties.getProperty("resource-pack");
            if (packUrl != null && !packUrl.isEmpty()) {
                getLogger().severe("You have a resource pack set in server.properties!");
                getLogger().severe("This will cause ForcePack to not function correctly. You MUST remove the resource pack URL from server.properties!");
            }
        }
    }

    public Translator getTranslator() {
        return translator;
    }

    public boolean debug() {
        return getConfig().getBoolean("Server.debug");
    }

    @Override
    public void log(String info, Object... format) {
        if (debug()) getLogger().info(String.format(info, format));
    }

    public static ForcePackAPI getAPI() {
        return getInstance();
    }

    public static ForcePackPaper getInstance() {
        return getPlugin(ForcePackPaper.class);
    }

}
