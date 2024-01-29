package com.convallyria.forcepack.sponge;

import com.convallyria.forcepack.api.ForcePackAPI;
import com.convallyria.forcepack.api.logger.ForcePackLogger;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.schedule.PlatformScheduler;
import com.convallyria.forcepack.sponge.schedule.SpongeScheduler;
import com.google.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.spongepowered.api.Server;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.StartedEngineEvent;
import org.spongepowered.plugin.PluginContainer;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.util.Set;

@Plugin("forcepack")
public class ForcePackSponge implements ForcePackAPI {

    @Inject
    private PluginContainer pluginContainer;

    @Inject
    private Logger logger;

    private SpongeScheduler scheduler;

    @Listener
    public void onServerStart(final StartedEngineEvent<Server> event) {

        this.scheduler = new SpongeScheduler(this);
    }

    public PluginContainer pluginContainer() {
        return pluginContainer;
    }

    @Override
    public Set<ResourcePack> getResourcePacks() {
        return null;
    }

    @Override
    public PlatformScheduler getScheduler() {
        return scheduler;
    }
}
