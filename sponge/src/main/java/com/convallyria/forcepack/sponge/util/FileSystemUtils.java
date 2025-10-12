package com.convallyria.forcepack.sponge.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class FileSystemUtils {

	private FileSystemUtils() {
		throw new UnsupportedOperationException("This class cannot be instantiated");
	}

	/**
	 * Visits the resources at the given {@link Path} within the resource
	 * path of the given {@link Class}.
	 *
	 * @param target				  The target class of the resource path to scan
	 * @param consumer				The consumer to visit the resolved path
	 * @param firstPathComponent	  First path component
	 * @param remainingPathComponents Remaining path components
	 */
	public static boolean visitResources(final Class<?> target, final Consumer<Path> consumer, final String firstPathComponent, final String... remainingPathComponents) throws IOException {
		final URL knownResource = FileSystemUtils.class.getClassLoader().getResource("assets/forcepack/config.yml");
		if (knownResource == null) {
			throw new IllegalStateException("config.yml does not exist, don't know where we are");
		}
		if (knownResource.getProtocol().equals("jar")) {
			// Running from a JAR
			final String jarPathRaw = Iterables.get(Splitter.on('!').split(knownResource.toString()), 0);
			final URI path = URI.create(jarPathRaw + "!/");

//			try (final FileSystem fileSystem = FileSystems.newFileSystem(path, Map.of("create", "true"))) {
//				final Path toVisit = fileSystem.getPath(firstPathComponent, remainingPathComponents);
//				if (Files.exists(toVisit)) {
//					consumer.accept(toVisit);
//					return true;
//				}
//				return false;
//			}

			final FileSystem fileSystem = FileSystems.getFileSystem(path);
			final Path toVisit = fileSystem.getPath(firstPathComponent, remainingPathComponents);
			if (Files.exists(toVisit)) {
				consumer.accept(toVisit);
				return true;
			}
			return false;
		} else {
			// Running from the file system
			final URI uri;
			final List<String> componentList = new ArrayList<>();
			componentList.add(firstPathComponent);
			componentList.addAll(Arrays.asList(remainingPathComponents));

			try {
				final URL url = target.getClassLoader().getResource(String.join("/", componentList));
				if (url == null) {
					return false;
				}
				uri = url.toURI();
			} catch (final URISyntaxException e) {
				throw new IllegalStateException(e);
			}
			consumer.accept(Paths.get(uri));
			return true;
		}
	}
}
