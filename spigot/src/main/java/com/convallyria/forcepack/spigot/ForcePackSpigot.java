package com.convallyria.forcepack.spigot;

import com.convallyria.forcepack.api.ForcePackAPI;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.schedule.PlatformScheduler;
import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.api.utils.HashingUtil;
import com.convallyria.forcepack.api.verification.ResourcePackURLData;
import com.convallyria.forcepack.folia.schedule.FoliaScheduler;
import com.convallyria.forcepack.spigot.command.Commands;
import com.convallyria.forcepack.spigot.integration.ItemsAdderIntegration;
import com.convallyria.forcepack.spigot.listener.ExemptionListener;
import com.convallyria.forcepack.spigot.listener.ResourcePackListener;
import com.convallyria.forcepack.spigot.listener.VelocityMessageListener;
import com.convallyria.forcepack.spigot.resourcepack.SpigotResourcePack;
import com.convallyria.forcepack.spigot.schedule.BukkitScheduler;
import com.convallyria.forcepack.spigot.translation.Translations;
import com.convallyria.languagy.api.language.Language;
import com.convallyria.languagy.api.language.Translator;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;

public final class ForcePackSpigot extends JavaPlugin implements ForcePackAPI {

    private Translator translator;
    private PlatformScheduler scheduler;
    private ResourcePack resourcePack;
    public boolean velocityMode;

    @Override
    public List<ResourcePack> getResourcePacks() {
        return List.of(resourcePack);
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler;
    }

    private final Map<UUID, ResourcePack> waiting = new HashMap<>();

    public Map<UUID, ResourcePack> getWaiting() {
        return waiting;
    }

    @Override
    public void onEnable() {
        this.generateLang();
        this.createConfig();
        this.velocityMode = getConfig().getBoolean("velocity-mode");
        this.scheduler = FoliaScheduler.RUNNING_FOLIA ? new FoliaScheduler(this) : new BukkitScheduler(this);
        this.registerListeners();
        this.registerCommands();
        this.translator = Translator.of(this, "lang", Language.BRITISH_ENGLISH, debug());

        // Convert legacy config
        try {
            this.performLegacyCheck();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Runnable run = () -> {
            if (!reload()) {
                getLogger().severe("Unable to load ForcePack correctly.");
                return;
            }

            new Metrics(this, 13677);
            this.getLogger().info("[ForcePack] Enabled!");
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
    }

    @Override
    public void onDisable() {
        translator.close();
    }

    public boolean reload() {
        if (velocityMode) return true;
        String url = getConfig().getString("Server.ResourcePack.url", "");
        String hash = getConfig().getString("Server.ResourcePack.hash", "");
        AtomicInteger sizeMB = new AtomicInteger();

        List<String> validUrlEndings = Arrays.asList(".zip", ".zip?dl=1");
        boolean hasEnding = false;
        for (String validUrlEnding : validUrlEndings) {
            if (url.endsWith(validUrlEnding)) {
                hasEnding = true;
                break;
            }
        }

        if (!hasEnding) {
            getLogger().severe("Your URL has an invalid or unknown format. " +
                    "URLs must have no redirects and use the .zip extension. If you are using Dropbox, change ?dl=0 to ?dl=1.");
            getLogger().severe("ForcePack will still load in the event this check is incorrect. Please make an issue or pull request if this is so.");
        }

        ResourcePackURLData data = null;
        if (getConfig().getBoolean("Server.ResourcePack.generate-hash")) {
            getLogger().info("Auto-generating ResourcePack hash.");
            try {
                data = HashingUtil.performPackCheck(url, hash);
                sizeMB.set(data.getSize());
                hash = data.getUrlHash();
                getLogger().info("Size of ResourcePack: " + sizeMB.get() + " MB");
                getLogger().info("Auto-generated ResourcePack hash: " + hash);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Unable to auto-generate ResourcePack hash, reverting to config setting", e);
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

                if (!hash.equalsIgnoreCase(data.getUrlHash())) {
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

        final int versionNumber = getVersionNumber();
        getLogger().info("Detected server version: " + Bukkit.getBukkitVersion() + " (" + getVersionNumber() + ").");
        if (versionNumber >= 18) {
            getLogger().info("Using recent ResourcePack methods to show prompt text.");
        } else {
            getLogger().warning("Your server version does not support prompt text.");
        }

        resourcePack = new SpigotResourcePack(this, url, hash, sizeMB.get());
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
    }

    public Translator getTranslator() {
        return translator;
    }

    public boolean debug() {
        return getConfig().getBoolean("Server.debug");
    }

    public void log(String info) {
        if (debug()) getLogger().info(info);
    }

    public static ForcePackAPI getAPI() {
        return getInstance();
    }

    public static ForcePackSpigot getInstance() {
        return getPlugin(ForcePackSpigot.class);
    }

    public int getVersionNumber() {
        String[] split = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
        return Integer.parseInt(split[1]);
    }
}
