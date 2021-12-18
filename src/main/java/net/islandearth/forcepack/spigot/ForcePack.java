package net.islandearth.forcepack.spigot;

import io.papermc.lib.PaperLib;
import net.islandearth.forcepack.spigot.api.ForcePackAPI;
import net.islandearth.forcepack.spigot.listener.ResourcePackListener;
import net.islandearth.forcepack.spigot.resourcepack.ResourcePack;
import net.islandearth.forcepack.spigot.resourcepack.SpigotResourcePack;
import net.islandearth.forcepack.spigot.translation.Translations;
import net.islandearth.forcepack.spigot.utils.HashingUtil;
import net.islandearth.forcepack.spigot.utils.TriConsumer;
import net.islandearth.languagy.api.language.Language;
import net.islandearth.languagy.api.language.LanguagyImplementation;
import net.islandearth.languagy.api.language.LanguagyPluginHook;
import net.islandearth.languagy.api.language.Translator;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ForcePack extends JavaPlugin implements ForcePackAPI, LanguagyPluginHook {

	@LanguagyImplementation(Language.ENGLISH)
	private Translator translator;

	private ResourcePack resourcePack;

	public ResourcePack getResourcePack() {
		return resourcePack;
	}

	@Override
	public void onEnable() {
		this.generateLang();
		this.createConfig();
		this.registerListeners();
		this.hook(this);

		// Convert legacy config
		try {
			this.performLegacyCheck();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			performPackCheck((url, config, match) -> {
				if (!match) {
					this.getLogger().severe("-----------------------------------------------");
					this.getLogger().severe("Your hash does not match the URL file provided!");
					this.getLogger().severe("The URL bytes: " + Arrays.toString(url));
					this.getLogger().severe("Your config bytes: " + Arrays.toString(config));
					this.getLogger().severe("Please provide a correct SHA-1 hash!");
					this.getLogger().severe("-----------------------------------------------");
				}
			});
		} catch (Exception e) {
			this.getLogger().severe("Please provide a correct SHA-1 hash/url!");
			e.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}

		final int versionNumber = getVersionNumber();
		getLogger().info("Detected server version: " + Bukkit.getBukkitVersion() + " (" + getVersionNumber() + ").");
		if (versionNumber >= 18) {
			getLogger().info("Using recent ResourcePack methods to show prompt text.");
		} else {
			getLogger().info("Your server version does not support prompt text.");
		}

		final String url = getConfig().getString("Server.ResourcePack.url");
		final String hash = getConfig().getString("Server.ResourcePack.hash");
		resourcePack = new SpigotResourcePack(this, url, hash);

		PaperLib.suggestPaper(this);
		this.getLogger().info("[ForcePack] Enabled!");
	}
	
	private void registerListeners() {
		PluginManager pm = Bukkit.getPluginManager();
		pm.registerEvents(new ResourcePackListener(this), this);
	}

	private void generateLang() {
		Translations.generateLang(this);
	}

	private void createConfig() {
		saveDefaultConfig();
	}

	private void performPackCheck(TriConsumer<byte[], byte[], Boolean> consumer) throws Exception {
		// This is not done async on purpose. We don't want the server to start without having checked this first.
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		InputStream fis = new URL(getConfig().getString("Server.ResourcePack.url")).openStream();
		int n = 0;
		byte[] buffer = new byte[8192];
		while (n != -1) {
			n = fis.read(buffer);
			if (n > 0) {
				digest.update(buffer, 0, n);
			}
		}
		fis.close();
		final byte[] urlBytes = digest.digest();
		final byte[] configBytes = HashingUtil.toByteArray(getConfig().getString("Server.ResourcePack.hash"));
		consumer.accept(urlBytes, configBytes, Arrays.equals(urlBytes, configBytes));
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

	@Override
	public Translator getTranslator() {
		return translator;
	}

	@Override
	public void onLanguagyHook() { }

	@Override
	public boolean debug() {
		return getConfig().getBoolean("debug");
	}

	public void log(String info) {
		if (debug()) getLogger().info(info);
	}

	public static ForcePackAPI getAPI() {
		return getPlugin(ForcePack.class);
	}

	public int getVersionNumber() {
		String[] split = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
		return Integer.parseInt(split[1]);
	}
}
