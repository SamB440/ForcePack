package net.islandearth.forcepack.sponge;

import java.nio.file.Path;

import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;

import com.google.inject.Inject;

import net.islandearth.forcepack.sponge.config.ConfigurationManager;
import net.islandearth.forcepack.sponge.listener.ResourcePackListener;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@Plugin(id = "forcepack", name = "ForcePack", version = "1.0.0", description = "Forces players to use your resourcepack")
public class SpongeForcePack {
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	private Path defaultConfig;

	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;
	
	private ConfigurationManager config;
	
	@Listener
	public void onStart(GameStartedServerEvent gse) {
		this.config = new ConfigurationManager();
		config.setup(defaultConfig.toFile(), configManager);
		registerListeners();
	}
	
	@Listener
	public void reload(GameReloadEvent event) {
	    //called when server reloads
	}
	
	/**
	 * Instantiates all listeners
	 */
	private void registerListeners() {
		new ResourcePackListener(this);
	}
	
	/**
	 * @return {@link ConfigurationManager}, null if not set
	 */
	public ConfigurationManager getConfig() {
		return config;
	}
}
