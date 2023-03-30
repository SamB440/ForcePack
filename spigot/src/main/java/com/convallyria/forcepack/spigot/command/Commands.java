package com.convallyria.forcepack.spigot.command;

import cloud.commandframework.annotations.AnnotationParser;
import cloud.commandframework.arguments.parser.ParserParameters;
import cloud.commandframework.arguments.parser.StandardParameters;
import cloud.commandframework.bukkit.CloudBukkitCapabilities;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import org.bukkit.command.CommandSender;

import java.util.function.Function;

public class Commands {

    public Commands(ForcePackSpigot plugin) {

        // This function maps the command sender type of our choice to the bukkit command sender.
        final Function<CommandSender, CommandSender> mapperFunction = Function.identity();

        final PaperCommandManager<CommandSender> manager;
        try {
            manager = new PaperCommandManager<>(
                    plugin,
                    CommandExecutionCoordinator.simpleCoordinator(),
                    mapperFunction,
                    mapperFunction
            );
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize the command manager");
            e.printStackTrace();
            return;
        }

        // Register Brigadier mappings
        if (manager.hasCapability(CloudBukkitCapabilities.BRIGADIER)) {
            manager.registerBrigadier();
        }

        // Register asynchronous completions
        if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
            manager.registerAsynchronousCompletions();
        }

        // This will allow you to decorate commands with descriptions
        final Function<ParserParameters, CommandMeta> commandMetaFunction = parserParameters ->
                CommandMeta.simple()
                        .with(CommandMeta.DESCRIPTION, parserParameters.get(StandardParameters.DESCRIPTION, "No description"))
                        .build();
        final AnnotationParser<CommandSender> annotationParser = new AnnotationParser<>(manager, CommandSender.class, commandMetaFunction);

        annotationParser.parse(new ForcePackCommand(plugin));
    }
}
