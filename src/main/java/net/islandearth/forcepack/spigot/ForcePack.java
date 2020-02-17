package net.islandearth.forcepack.spigot;

import net.islandearth.forcepack.spigot.api.ForcePackAPI;
import net.islandearth.forcepack.spigot.listener.ResourcePackListener;
import net.islandearth.forcepack.spigot.translation.Translations;
import net.islandearth.languagy.language.Language;
import net.islandearth.languagy.language.LanguagyImplementation;
import net.islandearth.languagy.language.LanguagyPluginHook;
import net.islandearth.languagy.language.Translator;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;

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
		saveConfig();
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
}
