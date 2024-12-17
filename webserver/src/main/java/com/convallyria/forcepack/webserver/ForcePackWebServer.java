package com.convallyria.forcepack.webserver;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.util.JavalinLogger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ForcePackWebServer {

    private final Javalin app;
    private final Path dataFolder;
    private final boolean usePort;
    private final String protocol;
    private final String ipAddress;
    private final int port;
    private final Map<File, String> hostedPacks = new HashMap<>();

    public ForcePackWebServer(Path dataFolder, String protocol, String serverIp, int port, Boolean usePort) throws IOException {
        JavalinLogger.enabled = false;
        JavalinLogger.startupInfo = false;
        this.app = Javalin.create(config -> config.showJavalinBanner = false).start(port);
        this.dataFolder = dataFolder;
        setupEndpoints();
        this.usePort = usePort;
        this.protocol = protocol;
        this.ipAddress = serverIp;
        this.port = port;
    }

    public void addHostedPack(File hostedPack) {
        try {
            this.hostedPacks.put(hostedPack, Files.asByteSource(hostedPack).hash(Hashing.sha1()).toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearHostedPacks() {
        this.hostedPacks.clear();
    }

    public void shutdown() {
        app.stop();
    }

    public String getUrl() {
        return protocol + ipAddress + (usePort? ":" + port : "");
    }

    public String getHostedEndpoint(String urlString) {
        final File targetFile = new File(dataFolder + File.separator + urlString.replace("forcepack://", ""));
        return getUrl() + "/serve/" + hostedPacks.get(targetFile) + ".zip";
    }

    private final Cache<String, Runnable> waitingServes = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES)
            .build();

    public void awaitServe(String id, Runnable runnable) {
        waitingServes.put(id, runnable);
    }

    private void setupEndpoints() {
        app.get("/", ctx -> ctx.status(HttpStatus.NO_CONTENT));

        app.get("/serve/<id>", ctx -> {
            final String id = ctx.pathParam("id");
            // Headers
            // {X-Minecraft-Version=1.20.1, X-Minecraft-Version-ID=1.20.1, X-Minecraft-Username=Cotander,
            // Accept=text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2,
            // X-Minecraft-Pack-Format=15, User-Agent=Minecraft Java/1.20.1, Connection=keep-alive,
            // X-Minecraft-UUID=4b319cd4e8274dcfa3039a3fce310755, Host=localhost:2222}

            // If this is the real resource pack
            for (File hostedPack : hostedPacks.keySet()) {
                if (id.equals(hostedPacks.get(hostedPack) + ".zip")) {
                    final byte[] fileBytes = Files.asByteSource(hostedPack).read();
                    ctx.result(fileBytes)
                            .header("X-Hosted-By", "forcepack")
                            .header("Content-Type", "application/zip")
                            .header("Content-Disposition", "attachment; filename=" + hostedPack.getName());
                    return;
                }
            }

            ctx.status(HttpStatus.OK)
                    .header("X-Hosted-By", "forcepack")
                    .header("Content-Type", "application/zip")
                    .header("Content-Disposition", "attachment; filename=" + id);

            final Runnable runnable = waitingServes.getIfPresent(id);
            waitingServes.invalidate(id);
            if (runnable != null) {
                runnable.run();
            }
        });
    }

    public static String getIp() {
        // Copied from https://github.com/oraxen/oraxen/pull/986
        try {
            URL url = new URL("https://api.ipify.org");
            InputStream stream = url.openStream();
            Scanner s = new Scanner(stream, StandardCharsets.UTF_8).useDelimiter("\\A");
            String ip = s.next();
            s.close();
            stream.close();
            return ip;
        } catch (IOException e) {
            e.printStackTrace();
            return "localhost";
        }
    }
}
