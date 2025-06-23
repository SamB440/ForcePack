package com.convallyria.forcepack.sponge.command;

import com.convallyria.forcepack.sponge.ForcePackSponge;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.sponge.SpongeCommandManager;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.registry.RegistryHolder;

public class Commands {

    public Commands(ForcePackSponge plugin, RegistryHolder registryHolder) {

        final SpongeCommandManager<CommandCause> manager;
        try {
            manager = new SpongeCommandManager<>(
                    plugin.pluginContainer(),
                    ExecutionCoordinator.simpleCoordinator(),
                    registryHolder,
                    SenderMapper.identity()
            );
        } catch (Exception e) {
            plugin.getLogger().error("Failed to initialize the command manager", e);
            return;
        }

        // This will allow you to decorate commands with descriptions
        final AnnotationParser<CommandCause> annotationParser = new AnnotationParser<>(manager, CommandCause.class);
        annotationParser.parse(new ForcePackCommand(plugin));
    }
}
