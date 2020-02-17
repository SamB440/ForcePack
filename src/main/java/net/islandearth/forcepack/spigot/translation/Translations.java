package net.islandearth.forcepack.spigot.translation;

import net.islandearth.forcepack.spigot.ForcePack;
import net.islandearth.languagy.language.Language;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public enum Translations {
	DECLINED("&cYou must accept the resource pack to play on our server. Don't know how? Check out &ehttps://samb440.gitlab.io/resourcepack.html."),
	ACCEPTED("&aThank you for accepting our resource pack! You can now play."),
	DOWNLOAD_FAILED("&cThe resource pack download failed. Please reconnect and try again.");

	private final String defaultValue;
	private final boolean isList;
	
	Translations(String defaultValue) {
		this.defaultValue = defaultValue;
		this.isList = false;
	}

	Translations(String defaultValue, boolean isList) {
		this.defaultValue = defaultValue;
		this.isList = isList;
	}
	
	public String getDefaultValue() {
		return defaultValue;
	}

	public boolean isList() {
		return isList;
	}

	private String getPath() {
		return this.toString().toLowerCase();
	}

	public void send(Player player) {
		String message = ForcePack.getAPI().getTranslator().getTranslationFor(player, this.getPath());
		player.sendMessage(message);
	}

	public void send(Player player, String... values) {
		String message = ForcePack.getAPI().getTranslator().getTranslationFor(player, this.getPath());
		message = replaceVariables(message, values);
		player.sendMessage(message);
	}

	public void sendList(Player player) {
		List<String> message = ForcePack.getAPI().getTranslator().getTranslationListFor(player, this.getPath());
		message.forEach(player::sendMessage);
	}

	public void sendList(Player player, String... values) {
		List<String> messages = ForcePack.getAPI().getTranslator().getTranslationListFor(player, this.getPath());
		messages.forEach(message -> {
			message = replaceVariables(message, values);
			player.sendMessage(message);
		});
	}

	public String get(Player player) {
		return ForcePack.getAPI().getTranslator().getTranslationFor(player, this.getPath());
	}
	
	public String get(Player player, String... values) {
		String message = ForcePack.getAPI().getTranslator().getTranslationFor(player, this.getPath());
		message = replaceVariables(message, values);
		return message;
	}

	public List<String> getList(Player player) {
		return ForcePack.getAPI().getTranslator().getTranslationListFor(player, this.getPath());
	}

	public List<String> getList(Player player, String... values) {
		List<String> messages = new ArrayList<>();
		ForcePack.getAPI().getTranslator()
				.getTranslationListFor(player, this.getPath())
				.forEach(message -> messages.add(replaceVariables(message, values)));
		return messages;
	}
	
	public static void generateLang(ForcePack plugin) {
		File lang = new File(plugin.getDataFolder() + "/lang/");
		lang.mkdirs();
		
		for (Language language : Language.values()) {
			try {
				plugin.saveResource("lang/" + language.getCode() + ".yml", false);
				plugin.getLogger().info("Generated " + language.getCode() + ".yml");
			} catch (IllegalArgumentException ignored) { }

			File file = new File(plugin.getDataFolder() + "/lang/" + language.getCode() + ".yml");
			if (file.exists()) {
				FileConfiguration config = YamlConfiguration.loadConfiguration(file);
				for (Translations key : values()) {
					if (config.get(key.toString().toLowerCase()) == null) {
						plugin.getLogger().warning("No value in translation file for key "
								+ key.toString() + " was found. Regenerate language files?");
					}
				}
			}
		}
	}

	private String replaceVariables(String message, String... values) {
		String modifiedMessage = message;
		for (int i = 0; i < 10; i++) {
			if (values.length > i) modifiedMessage = modifiedMessage.replaceAll("%" + i, values[i]);
			else break;
		}

		return modifiedMessage;
	}
}
