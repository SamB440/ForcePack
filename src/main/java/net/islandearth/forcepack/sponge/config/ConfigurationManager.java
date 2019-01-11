package net.islandearth.forcepack.sponge.config;

import java.io.File;
import java.io.IOException;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

public class ConfigurationManager {
	
	private ConfigurationLoader<CommentedConfigurationNode> configLoader;
	private CommentedConfigurationNode config;
	
	public void setup(File configFile, ConfigurationLoader<CommentedConfigurationNode> configLoader) {
		this.configLoader = configLoader;
		
		if (!configFile.exists()) {
			try {
				configFile.createNewFile();
				loadConfig();
				saveConfig();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		loadConfig();
		saveValue("Declined", 
				"Message when pack is declined", 
				"&cYou must accept the resource pack to play on our server.");
		saveValue("Accepted", 
				"Message when pack is accepted", 
				"&aThank you for accepting our resource pack! You can now play.");
		saveValue("Failed", 
				"Message when pack fails to download", 
				"&cThe resource pack download failed. Please reconnect and try again.");
		saveConfig();
	}
	
	public CommentedConfigurationNode getConfig() {
		return config;
	}
	
	public void saveConfig() {
		try {
			configLoader.save(config);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadConfig() {
		try {
			config = configLoader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void saveValue(String node, String comment, Object value) {
		if (config.getNode(node).getString() == null) {
			config.getNode(node)
				.setComment(comment)
				.setValue(value);
		}
	}
}