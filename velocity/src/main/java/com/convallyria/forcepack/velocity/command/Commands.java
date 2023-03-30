package com.convallyria.forcepack.velocity.command;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.velocity.VelocityCommandManager;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.plugin.PluginContainer;

import java.util.function.Function;

public class Commands {

    public Commands(ForcePackVelocity plugin, PluginContainer container) {

        // This function maps the command sender type of our choice to the bukkit command sender.
        final Function<CommandSource, CommandSource> mapperFunction = Function.identity();

        final VelocityCommandManager<CommandSource> manager;
        try {
            manager = new VelocityCommandManager<>(
                    container,
                    plugin.getServer(),
                    CommandExecutionCoordinator.simpleCoordinator(),
                    mapperFunction,
                    mapperFunction
            );
        } catch (Exception e) {
            plugin.getLogger().error("Failed to initialize the command manager");
            e.printStackTrace();
            return;
        }

        // This will allow you to decorate commands with descriptions
        final Function<ParserParameters, CommandMeta> commandMetaFunction = parserParameters ->
                CommandMeta.simple()
                        .with(CommandMeta.DESCRIPTION, parserParameters.get(StandardParameters.DESCRIPTION, "No description"))
                        .build();
        final AnnotationParser<CommandSource> annotationParser = new AnnotationParser<>(manager, CommandSource.class, commandMetaFunction);

        annotationParser.parse(new ForcePackCommand(plugin));
    }
}
