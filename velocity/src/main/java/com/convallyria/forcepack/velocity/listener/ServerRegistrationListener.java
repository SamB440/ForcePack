package com.convallyria.forcepack.velocity.listener;

import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.convallyria.forcepack.velocity.config.VelocityConfig;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.server.ServerRegisteredEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.List;
import java.util.Map;

public class ServerRegistrationListener {

    private final ForcePackVelocity plugin;

    public ServerRegistrationListener(ForcePackVelocity plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onServerRegistered(ServerRegisteredEvent event) {
        RegisteredServer registeredServer = event.registeredServer();
        String serverName = registeredServer.getServerInfo().getName();
        VelocityConfig groups = plugin.getConfig().getConfig("groups");

        if (groups == null) return;

        // Check if we already have packs for this server
        boolean hasPack = plugin.getResourcePacks().stream()
                .anyMatch(pack -> pack.getServer().equals(serverName));

        if (hasPack) return;

        for (String groupName : groups.getKeys()) {
            VelocityConfig groupConfig = groups.getConfig(groupName);
            List<String> servers = groupConfig.getStringList("servers");
            boolean exact = groupConfig.getBoolean("exact-match");

            boolean matches = exact ?
                    servers.contains(serverName) :
                    servers.stream().anyMatch(serverName::contains);

            if (matches) {
                plugin.log("New server %s matches group %s, adding resource packs...", serverName, groupName);

                final Map<String, VelocityConfig> configs = plugin.getPackConfigs(groupConfig, groupName);

                configs.forEach((id, config) -> {
                    if (config == null) return;
                    plugin.registerResourcePackForServer(config, id, groupName, "group", plugin.getConfig().getBoolean("verify-resource-packs"), serverName, null);
                });
            }
        }
    }
}
