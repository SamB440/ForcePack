package net.islandearth.forcepack.spigot;

import io.papermc.lib.PaperLib;
import net.islandearth.forcepack.spigot.api.ForcePackAPI;
import net.islandearth.forcepack.spigot.listener.ResourcePackListener;
import net.islandearth.forcepack.spigot.translation.Translations;
import net.islandearth.forcepack.spigot.utils.HashingUtil;
import net.islandearth.languagy.api.language.Language;
import net.islandearth.languagy.api.language.LanguagyImplementation;
import net.islandearth.languagy.api.language.LanguagyPluginHook;
import net.islandearth.languagy.api.language.Translator;
import org.apache.logging.log4j.util.TriConsumer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;

public final class ForcePack extends JavaPlugin implements ForcePackAPI, LanguagyPluginHook {

	@LanguagyImplementation(Language.ENGLISH)
	private Translator translator;
	private static ForcePack plugin;

	@Override
	public void onEnable() {
		plugin = this;
		this.generateLang();
		this.createConfig();
		this.registerListeners();
		this.hook(this);
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
		}
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
		getConfig().options().copyDefaults(true);
		getConfig().addDefault("Server.ResourcePack.url", "https://faithfulpack.com/dl/104/");
		getConfig().addDefault("Server.ResourcePack.hash", "BA52452AD77CAAA7530876950C8E2D020699EBDD");
		getConfig().addDefault("Server.Actions.On_Accept.Command", Collections.singletonList("say [player] accepted the resource pack!"));
		getConfig().addDefault("Server.Actions.On_Deny.Command", Collections.singletonList("say [player] denied the resource pack!"));
		getConfig().addDefault("Server.Actions.On_Fail.Command", Collections.singletonList("say [player] failed to download the resource pack!"));
		getConfig().addDefault("Server.kick", true);
		getConfig().addDefault("Server.Update GUI Speed", 20);
		saveConfig();
	}

	private void performPackCheck(TriConsumer<byte[], byte[], Boolean> consumer) throws Exception {
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

	@Override
	public Translator getTranslator() {
		return translator;
	}

	@Override
	public void onLanguagyHook() { }

	public static ForcePackAPI getAPI() {
		return plugin;
	}

	@Override
	public boolean debug() {
		return true;
	}
}
