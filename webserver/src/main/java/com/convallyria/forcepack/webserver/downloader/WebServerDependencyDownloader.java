package com.convallyria.forcepack.webserver.downloader;

import com.convallyria.forcepack.api.ForcePackAPI;
import dev.vankka.dependencydownload.DependencyManager;
import dev.vankka.dependencydownload.repository.StandardRepository;

import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Collections;
import java.util.function.Consumer;

/**
 * Utility class to download the web server dependency jars. We do this because ForcePack will be 9MB otherwise.
 * <p></p>
 * The webserver jar is only downloaded if the webserver is enabled.
 */
public class WebServerDependencyDownloader {

    public static void download(ForcePackAPI api, Path dataFolder, Consumer<String> logger) throws IOException {
        File cacheFolder = new File(dataFolder + File.separator + "libraries");
        if (!cacheFolder.exists()) cacheFolder.mkdirs();
        DependencyManager manager = new DependencyManager(cacheFolder.toPath());
        manager.loadFromResource(api.getClass().getResource("/runtimeDownloadOnly.txt"));
        logger.accept("Downloading " + manager.getDependencies().size() + " dependencies...");
        manager.downloadAll(Runnable::run, Collections.singletonList(new StandardRepository("https://repo.maven.apache.org/maven2/"))).join();
        logger.accept("Finished downloading dependencies...");
        manager.relocateAll(Runnable::run).join();
        logger.accept("Relocating dependencies and adding to class loader...");
        manager.loadAll(Runnable::run, path -> {
            URLClassLoaderAccess access = URLClassLoaderAccess.create((URLClassLoader) api.getClass().getClassLoader());
            access.addURL(path.toUri().toURL());
        });
    }
}
