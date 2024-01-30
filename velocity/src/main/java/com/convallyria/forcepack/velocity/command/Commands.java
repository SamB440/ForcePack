package com.convallyria.forcepack.velocity.command;

import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.velocity.VelocityCommandManager;

public class Commands {

    public Commands(ForcePackVelocity plugin, PluginContainer container) {
        final VelocityCommandManager<CommandSource> manager;
        try {
            manager = new VelocityCommandManager<>(
                    container,
                    plugin.getServer(),
                    ExecutionCoordinator.simpleCoordinator(),
                    SenderMapper.identity()
            );
        } catch (Exception e) {
            plugin.getLogger().error("Failed to initialize the command manager", e);
            return;
        }

        final AnnotationParser<CommandSource> annotationParser = new AnnotationParser<>(manager, CommandSource.class);
        annotationParser.parse(new ForcePackCommand(plugin));
    }
}
