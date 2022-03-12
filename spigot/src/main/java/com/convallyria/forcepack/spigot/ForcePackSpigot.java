package com.convallyria.forcepack.spigot;

import co.aikar.commands.PaperCommandManager;
import com.convallyria.forcepack.api.ForcePackAPI;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.utils.HashingUtil;
import com.convallyria.forcepack.spigot.command.ForcePackCommand;
import com.convallyria.forcepack.spigot.listener.ResourcePackListener;
import com.convallyria.forcepack.spigot.resourcepack.SpigotResourcePack;
import com.convallyria.forcepack.spigot.translation.Translations;
import com.convallyria.languagy.api.language.Translator;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class ForcePackSpigot extends JavaPlugin implements ForcePackAPI {

    private Translator translator;
    private ResourcePack resourcePack;

    @Override
    public List<ResourcePack> getResourcePacks() {
        return List.of(resourcePack);
    }

    private final Map<UUID, ResourcePack> waiting = new HashMap<>();

    public Map<UUID, ResourcePack> getWaiting() {
        return waiting;
    }

    @Override
    public void onEnable() {
        this.generateLang();
        this.createConfig();
        this.registerListeners();
        this.registerCommands();
        this.translator = Translator.of(this).debug(debug());

        // Convert legacy config
        try {
            this.performLegacyCheck();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!reload()) return;

        new Metrics(this, 13677);
        this.getLogger().info("[ForcePack] Enabled!");
    }

    @Override
    public void onDisable() {
        translator.close();
    }

    public boolean reload() {
        final String url = getConfig().getString("Server.ResourcePack.url");
        String hash = getConfig().getString("Server.ResourcePack.hash");

        if (getConfig().getBoolean("Server.ResourcePack.generate-hash")) {
            getLogger().info("Auto-generating ResourcePack hash.");
            try {
                hash = HashingUtil.getHashFromUrl(url);
                getLogger().info("Auto-generated ResourcePack hash: " + hash);
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Unable to auto-generate ResourcePack hash, reverting to config setting", e);
            }
        }

        if (getConfig().getBoolean("Server.verify")) {
            try {
                HashingUtil.performPackCheck(url, hash, (urlHash, configHash, match) -> {
                    if (!match) {
                        this.getLogger().severe("-----------------------------------------------");
                        this.getLogger().severe("Your hash does not match the URL file provided!");
                        this.getLogger().severe("The URL hash returned: " + urlHash);
                        this.getLogger().severe("Your config hash returned: " + configHash);
                        this.getLogger().severe("Please provide a correct SHA-1 hash!");
                        this.getLogger().severe("-----------------------------------------------");
                    }
                });
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

        resourcePack = new SpigotResourcePack(this, url, hash);
        return true;
    }

    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new ResourcePackListener(this), this);
    }

    private void registerCommands() {
        PaperCommandManager manager = new PaperCommandManager(this);
        manager.enableUnstableAPI("help");
        manager.registerCommand(new ForcePackCommand(this));
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
