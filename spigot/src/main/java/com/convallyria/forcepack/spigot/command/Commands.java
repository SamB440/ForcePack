package com.convallyria.forcepack.spigot.command;

import com.convallyria.forcepack.spigot.ForcePackSpigot;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;

public class Commands {

    public Commands(ForcePackSpigot plugin) {

        final LegacyPaperCommandManager<CommandSender> manager;
        try {
            manager = new LegacyPaperCommandManager<>(
                    plugin,
                    ExecutionCoordinator.simpleCoordinator(),
                    SenderMapper.identity()
            );
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize the command manager");
            e.printStackTrace();
            return;
        }

        try {
            // Register brigadier or async completions
            if (manager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
                manager.registerBrigadier();
            } else if (manager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
                manager.registerAsynchronousCompletions();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to register brigadier/asynchronous completions, your server software is lying about its capabilities!");
        }

        // This will allow you to decorate commands with descriptions
        final AnnotationParser<CommandSender> annotationParser = new AnnotationParser<>(manager, CommandSender.class);
        annotationParser.parse(new ForcePackCommand(plugin));
    }
}
