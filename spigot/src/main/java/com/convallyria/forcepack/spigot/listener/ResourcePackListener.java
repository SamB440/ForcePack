package com.convallyria.forcepack.spigot.listener;

import com.convallyria.forcepack.api.permission.Permissions;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.schedule.PlatformScheduler;
import com.convallyria.forcepack.api.utils.ClientVersion;
import com.convallyria.forcepack.api.utils.GeyserUtil;
import com.convallyria.forcepack.spigot.ForcePackSpigot;
import com.convallyria.forcepack.spigot.translation.Translations;
import com.convallyria.forcepack.spigot.util.ViaVersionUtil;
import com.convallyria.forcepack.webserver.ForcePackWebServer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ResourcePackListener implements Listener {

    private final ForcePackSpigot plugin;

    private final Map<UUID, Long> sentAccept = new HashMap<>();
    private final Map<UUID, Consumer<Void>> sentFalsePack = new HashMap<>();

    public ResourcePackListener(final ForcePackSpigot plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent event) {
        final long now = System.currentTimeMillis();
        final Player player = event.getPlayer();
        boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        boolean canBypass = player.hasPermission(Permissions.BYPASS) && getConfig().getBoolean("Server.bypass-permission");
        plugin.log(player.getName() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");

        if (canBypass || geyser) {
            return;
        }

        final PlayerResourcePackStatusEvent.Status status = event.getStatus();
        plugin.log(player.getName() + " sent status: " + status);

        // Just pass declined onto the other handlers for the correct log message
        if (sentFalsePack.containsKey(player.getUniqueId()) && event.getStatus() != PlayerResourcePackStatusEvent.Status.DECLINED) {
            if (event.getStatus() == PlayerResourcePackStatusEvent.Status.ACCEPTED) return; // First, it gets accepted
            // We expect FAILED_DOWNLOAD, otherwise it must be SUCCESSFULLY_LOADED that was sent.
            if (event.getStatus() != PlayerResourcePackStatusEvent.Status.FAILED_DOWNLOAD) {
                plugin.log("Kicked player " + player.getName() + " because they are sending fake resource pack statuses (sent invalid status for fake resource pack).");
                ensureMainThread(() -> player.kickPlayer(Translations.DOWNLOAD_FAILED.get(player)));
                return;
            }

            final Consumer<Void> remove = sentFalsePack.remove(player.getUniqueId());
            remove.accept(null);
            ensureMainThread(player::closeInventory);
            return;
        }

        // Only remove from waiting if they actually loaded the resource pack, rather than any status
        // Declined/failed is valid and should be allowed, server owner decides whether they get kicked
        if (status != PlayerResourcePackStatusEvent.Status.ACCEPTED) {
            plugin.getWaiting().remove(player.getUniqueId());
        }

        for (String cmd : getConfig().getStringList("Server.Actions." + status.name() + ".Commands")) {
            ensureMainThread(() -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("[player]", player.getName())));
        }

        // Don't execute kicks - handled by proxy
        if (plugin.velocityMode) return;

        final boolean kick = getConfig().getBoolean("Server.Actions." + status.name() + ".kick");

        if (tryValidateHacks(player, status, now)) return;

        switch (status) {
            case DECLINED: {
                ensureMainThread(() -> {
                    if (kick) player.kickPlayer(Translations.DECLINED.get(player));
                    else Translations.DECLINED.send(player);
                });

                sentAccept.remove(player.getUniqueId());
                break;
            }
            case FAILED_DOWNLOAD: {
                ensureMainThread(() -> {
                    if (kick) player.kickPlayer(Translations.DOWNLOAD_FAILED.get(player));
                    else Translations.DOWNLOAD_FAILED.send(player);
                });

                sentAccept.remove(player.getUniqueId());
                break;
            }
            case SUCCESSFULLY_LOADED: {
                if (kick) ensureMainThread(() -> player.kickPlayer(Translations.ACCEPTED.get(player)));
                else {
                    ensureMainThread(() -> Translations.ACCEPTED.send(player));
                    boolean sendTitle = plugin.getConfig().getBoolean("send-loading-title");
                    if (sendTitle) player.sendTitle(null, null, 0, 0, 0); // resetTitle doesn't clear subtitle
                    sentFalsePack.remove(player.getUniqueId());
                }
                break;
            }
        }
    }

    private boolean tryValidateHacks(Player player, PlayerResourcePackStatusEvent.Status status, long now) {
        final boolean tryPrevent = getConfig().getBoolean("try-to-stop-fake-accept-hacks", true);
        if (status == PlayerResourcePackStatusEvent.Status.ACCEPTED) {
            if (tryPrevent && sentAccept.containsKey(player.getUniqueId())) {
                plugin.log("Kicked player " + player.getName() + " because they are sending fake resource pack statuses (accepted sent twice).");
                ensureMainThread(() -> player.kickPlayer(Translations.DECLINED.get(player)));
                return true;
            }
            if (tryPrevent) sentAccept.put(player.getUniqueId(), now);
        } else if (status == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            if (tryPrevent && !sentAccept.containsKey(player.getUniqueId())) {
                plugin.log("Kicked player " + player.getName() + " because they are sending fake resource pack statuses (order not maintained).");
                ensureMainThread(() -> player.kickPlayer(Translations.DOWNLOAD_FAILED.get(player)));
                return true;
            }

            // If a player is cheating and sends multiple status packets and tryPrevent is false, sentAccept may not contain the player
            // See issue https://github.com/SamB440/ForcePack/issues/9
            // Always set time to 11 if tryPrevent false to stop NullPointerException
            long time = !tryPrevent ? 11 : now - sentAccept.remove(player.getUniqueId());
            if (tryPrevent && time <= 10) {
                plugin.log("Kicked player " + player.getName() + " because they are sending fake resource pack statuses (sent too fast).");
                ensureMainThread(() -> player.kickPlayer(Translations.DOWNLOAD_FAILED.get(player)));
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean geyser = plugin.getConfig().getBoolean("Server.geyser") && GeyserUtil.isBedrockPlayer(player.getUniqueId());
        boolean canBypass = player.hasPermission(Permissions.BYPASS) && getConfig().getBoolean("Server.bypass-permission");
        plugin.log(player.getName() + "'s exemptions: geyser, " + geyser + ". permission, " + canBypass + ".");

        if (canBypass || geyser) return;

        if (plugin.velocityMode) {
            plugin.log("Velocity mode is enabled");
            plugin.getWaiting().put(player.getUniqueId(), null);
            return;
        }

        final ResourcePack pack = plugin.getPackForVersion(player);
        plugin.getWaiting().put(player.getUniqueId(), pack);

        final int version = ViaVersionUtil.getProtocolVersion(player);
        final int maxSize = ClientVersion.getMaxSizeForVersion(version);
        final boolean forceSend = getConfig().getBoolean("Server.force-invalid-size");
        if (!forceSend && pack.getSize() > maxSize) {
            if (plugin.debug()) plugin.getLogger().info(String.format("Not sending pack to %s because of excessive size for version %d (%dMB, %dMB).", player.getName(), version, pack.getSize(), maxSize));
            return;
        }

        if (getConfig().getBoolean("try-to-stop-fake-accept-hacks", true)) {
            this.sendFalsePack(player, pack, version);
        } else {
            this.runSetPackTask(player, pack, version);
        }
    }

    private void sendFalsePack(Player player, ResourcePack pack, int version) {
        final Optional<ForcePackWebServer> packWebServer = plugin.getWebServer();
        final String fakeFile = UUID.randomUUID().toString().replace("-", "") + ".zip";
        final String fakeUrl = packWebServer.map(server -> server.getUrl() + "/serve/" + fakeFile).orElse("https://");
        packWebServer.ifPresent(server -> server.awaitServe(fakeFile, () -> {
            if (!player.isOnline()) return;
            // The player has requested the resource pack.
            // So, we can send a close inventory packet along with the response.
            // This prevents the "pack application failed" screen from showing.
            // By sending it along with the pack response, we prevent slow connections seeing the screen for longer,
            //  as theoretically these packets should arrive at about the same time.
            //  (as long as the server is not lagging and depending on when the packets get flushed)
            // Most people won't notice it at all unless they are paying attention.
            ensureMainThread(player::closeInventory);
        }));

        plugin.log("Sending fake resource pack '" + fakeUrl + "' to " + player.getName() + ".");
        // Send the fake resource pack and a fake hash
        player.setResourcePack(fakeUrl, UUID.randomUUID().toString().replace("-", "").substring(0, 20).getBytes(StandardCharsets.UTF_8));

        sentFalsePack.put(player.getUniqueId(), (v) -> {
            if (!player.isOnline()) return;
            ensureMainThread(player::closeInventory); // Send close inventory packet immediately after
            this.runSetPackTask(player, pack, version);
        });
    }

    private void runSetPackTask(Player player, ResourcePack pack, int version) {
        AtomicReference<PlatformScheduler.ForcePackTask> task = new AtomicReference<>();
        Runnable packTask = () -> {
            if (plugin.getWaiting().containsKey(player.getUniqueId())) {
                plugin.log("Sent resource pack to player");
                pack.setResourcePack(player.getUniqueId());
            }

            boolean sendTitle = plugin.getConfig().getBoolean("send-loading-title");
            if (sendTitle && sentAccept.containsKey(player.getUniqueId())) {
                player.sendTitle(Translations.DOWNLOAD_START_TITLE.get(player), Translations.DOWNLOAD_START_SUBTITLE.get(player), 0, 30, 10);
            }

            final PlatformScheduler.ForcePackTask acquired = task.get();
            if (acquired != null && !plugin.getWaiting().containsKey(player.getUniqueId()) && !sentAccept.containsKey(player.getUniqueId())) {
                acquired.cancel();
            }
        };

        if (getConfig().getBoolean("Server.Update GUI") && version <= 340) { // 340 is 1.12
            task.set(plugin.getScheduler().executeRepeating(packTask, 0L, getConfig().getInt("Server.Update GUI Speed", 20)));
        } else {
            packTask.run();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent pqe) {
        Player player = pqe.getPlayer();
        plugin.getWaiting().remove(player.getUniqueId());
        sentAccept.remove(player.getUniqueId());
        sentFalsePack.remove(player.getUniqueId());
    }

    private void ensureMainThread(Runnable runnable) {
        plugin.getScheduler().executeOnMain(runnable);
    }

    private FileConfiguration getConfig() {
        return plugin.getConfig();
    }
}
