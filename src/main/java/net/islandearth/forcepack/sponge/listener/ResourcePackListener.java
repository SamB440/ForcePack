package net.islandearth.forcepack.sponge.listener;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.living.humanoid.player.ResourcePackStatusEvent;
import org.spongepowered.api.text.serializer.FormattingCodeTextSerializer;
import org.spongepowered.api.text.serializer.TextSerializers;

import net.islandearth.forcepack.sponge.SpongeForcePack;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;

public class ResourcePackListener {
	
	private SpongeForcePack plugin;
	
	public ResourcePackListener(SpongeForcePack plugin) {
		this.plugin = plugin;
		Sponge.getEventManager().registerListeners(plugin, this);
	}
	
	@Listener
	public void onPack(ResourcePackStatusEvent prpse) {
		Player player = prpse.getPlayer();
		if (!player.hasPermission("ForcePack.bypass")) {
			CommentedConfigurationNode config = plugin.getConfig().getConfig();
			FormattingCodeTextSerializer serializer = TextSerializers.FORMATTING_CODE;
			switch (prpse.getStatus()) {
				case FAILED: {
					player.kick(serializer.deserialize(config.getNode("Failed").getString()));
				}
				
				case DECLINED: {
					player.kick(serializer.deserialize(config.getNode("Declined").getString()));
					break;
				}
				
				case SUCCESSFULLY_LOADED:
				case ACCEPTED: {
					player.sendMessage(serializer.deserialize(config.getNode("Accepted").getString()));
					break;
				}
			}
		}
	}
}
