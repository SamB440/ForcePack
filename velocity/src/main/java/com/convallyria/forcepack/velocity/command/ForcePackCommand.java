package com.convallyria.forcepack.velocity.command;

import com.convallyria.forcepack.api.permission.Permissions;
import com.convallyria.forcepack.velocity.ForcePackVelocity;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Default;
import org.incendo.cloud.annotations.Flag;
import org.incendo.cloud.annotations.Permission;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForcePackCommand {

    private final ForcePackVelocity plugin;

    public ForcePackCommand(final ForcePackVelocity plugin) {
        this.plugin = plugin;
    }

    @CommandDescription("Default ForcePack command")
    @Command("vforcepack|velocityforcepack")
    public void onDefault(CommandSource sender) {
        sender.sendMessage(Component.text("ForcePack by SamB440. Type /vforcepack help for help.", NamedTextColor.GREEN));
    }

    @CommandDescription("Reloads the plugin config along with the resource pack")
    @Permission(Permissions.RELOAD)
    @Command("vforcepack|velocityforcepack reload [send]")
    public void onReload(CommandSource commandSource,
                         @Argument(value = "send", description = "Whether to send the updated resource pack to players") @Default("true") boolean send) {
        final CommandSource source = plugin.getServer().getConsoleCommandSource();
        final UUID possibleUUID = commandSource.pointers().getOrDefault(Identity.UUID, null);
        final Player sender = possibleUUID == null ? null : plugin.getServer().getPlayer(possibleUUID).orElse(null);
        Component reloadMsg = Component.text("Reloading...").color(NamedTextColor.GREEN);
        source.sendMessage(reloadMsg);
        if (sender != null) sender.sendMessage(reloadMsg);
        plugin.reloadConfig();
        plugin.loadResourcePacks(sender);

        if (send) {
            for (Player player : plugin.getServer().getAllPlayers()) {
                player.getCurrentServer().ifPresent(serverConnection ->
                        plugin.getPackHandler().setPack(player, serverConnection));
            }
        }

        Component doneMsg = Component.text("Done!").color(NamedTextColor.GREEN);
        source.sendMessage(doneMsg);
        if (sender != null) sender.sendMessage(doneMsg);
    }

    @CommandDescription("Updates the resource pack hash (and optional URL), saves config, and reloads packs for players")
    @Permission(Permissions.RELOAD)
    @Command("vforcepack|velocityforcepack update <hash>")
    public void onUpdate(CommandSource commandSource,
                         @Argument(value = "hash", description = "The new SHA-1 hash") String newHash,
                         @Flag("url") @Nullable String urlFlag,
                         @Flag("target") @Nullable String targetFlag) {
        // --url: optional new pack URL. --target: the table to edit, e.g. global-pack.version.18-
        // or servers.lobby (it and its sub-tables). Defaults to global-pack.
        final String newUrl = urlFlag == null ? "" : urlFlag.trim();
        final String target = targetFlag == null || targetFlag.isBlank() ? "global-pack" : targetFlag.trim();
        final CommandSource source = plugin.getServer().getConsoleCommandSource();
        final UUID possibleUUID = commandSource.pointers().getOrDefault(Identity.UUID, null);
        final Player sender = possibleUUID == null ? null : plugin.getServer().getPlayer(possibleUUID).orElse(null);

        String cleanHash = newHash.trim();
        if (cleanHash.startsWith("#")) {
            cleanHash = cleanHash.substring(1);
        }

        Component startMsg = Component.text("Updating resource pack hash to " + cleanHash + "...").color(NamedTextColor.YELLOW);
        source.sendMessage(startMsg);
        if (sender != null) sender.sendMessage(startMsg);

        try {
            Path file = plugin.getDataDirectory().resolve("config.toml");
            // Edit only the hash/url lines in place, so comments, formatting and the
            // rest of the file stay exactly as the server owner wrote them.
            int changed = updatePackLines(file, target, cleanHash, newUrl);
            if (changed == 0) {
                throw new IllegalStateException("no hash = \"...\" line found under [" + target + "] in config.toml");
            }

            plugin.getLogger().info("Saved new hash {} to config.toml", cleanHash);

            plugin.reloadConfig();
            plugin.loadResourcePacks(sender);

            for (Player player : plugin.getServer().getAllPlayers()) {
                player.getCurrentServer().ifPresent(serverConnection ->
                        plugin.getPackHandler().setPack(player, serverConnection));
            }

            Component doneMsg = Component.text("ForcePack hash updated to " + cleanHash + " and packs sent!").color(NamedTextColor.GREEN);
            source.sendMessage(doneMsg);
            if (sender != null) sender.sendMessage(doneMsg);

        } catch (Exception e) {
            plugin.getLogger().error("Failed to update config.toml: {}", e.getMessage(), e);
            Component errMsg = Component.text("Failed to update ForcePack: " + e.getMessage()).color(NamedTextColor.RED);
            source.sendMessage(errMsg);
            if (sender != null) sender.sendMessage(errMsg);
        }
    }

    private static final Pattern TABLE = Pattern.compile("^\\s*\\[+\\s*([^\\]]+?)\\s*\\]+\\s*(#.*)?$");
    private static final Pattern HASH = Pattern.compile("^(\\s*(?:resourcepack\\.)?hash\\s*=\\s*)\"[^\"]*\"(.*)$");
    private static final Pattern URL = Pattern.compile("^(\\s*(?:resourcepack\\.)?url\\s*=\\s*)\"[^\"]*\"(.*)$");

    /**
     * Replaces the value of every {@code hash = "..."} (and {@code url = "..."} when a URL is given) line
     * inside the {@code target} table and its sub-tables, leaving every other byte of the file untouched.
     *
     * @return how many lines were changed
     */
    static int updatePackLines(Path file, String target, String hash, String url) throws IOException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        String[] lines = text.split("(?<=\n)", -1);
        StringBuilder out = new StringBuilder(text.length() + 64);
        String table = "";
        int changed = 0;
        for (String raw : lines) {
            String eol = raw.endsWith("\r\n") ? "\r\n" : raw.endsWith("\n") ? "\n" : "";
            String line = raw.substring(0, raw.length() - eol.length());
            Matcher header = TABLE.matcher(line);
            if (header.matches()) {
                table = header.group(1);
            } else if (table.equals(target) || table.startsWith(target + ".")) {
                Matcher m = HASH.matcher(line);
                if (m.matches()) {
                    line = m.group(1) + "\"" + hash + "\"" + m.group(2);
                    changed++;
                } else if (!url.isEmpty() && (m = URL.matcher(line)).matches()) {
                    line = m.group(1) + "\"" + url.replace("\\", "\\\\").replace("\"", "\\\"") + "\"" + m.group(2);
                    changed++;
                }
            }
            out.append(line).append(eol);
        }
        if (changed > 0) {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, out, StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        return changed;
    }
}
