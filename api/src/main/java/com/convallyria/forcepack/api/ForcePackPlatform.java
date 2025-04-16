package com.convallyria.forcepack.api;

import com.convallyria.forcepack.api.resourcepack.PackFormatResolver;
import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.resourcepack.ResourcePackVersion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ForcePackPlatform extends ForcePackAPI {

    /**
     * Gets whether the specified URL is a default hosted one.
     * @param url the url to check
     * @return true if site is a default host
     */
    default boolean isDefaultHost(String url) {
        List<String> warnForHost = List.of("convallyria.com");
        for (String host : warnForHost) {
            if (url.contains(host)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets, if there is one, the blacklisted site within a URL
     * @param url the url to check
     * @return an {@link Optional<String>} possibly containing the URL of the blacklisted site found
     */
    default Optional<String> getBlacklistedSite(String url) {
        List<String> blacklisted = List.of("mediafire.com");
        for (String blacklistedSite : blacklisted) {
            if (url.contains(blacklistedSite)) {
                return Optional.of(url);
            }
        }
        return Optional.empty();
    }

    /**
     * Gets whether the specified URL has a valid ending
     * @param url the url to check
     * @return true if URL ends with a valid extension
     */
    default boolean isValidEnding(String url) {
        List<String> validUrlEndings = Arrays.asList(".zip", "dl=1");
        boolean hasEnding = false;
        for (String validUrlEnding : validUrlEndings) {
            if (url.endsWith(validUrlEnding)) {
                hasEnding = true;
                break;
            }
        }
        return hasEnding;
    }

    default ResourcePackVersion getVersionFromId(String versionId) {
        if (versionId.equals("all")) {
            return null;
        }

        try {
            // One version?
            final int fixedVersion = Integer.parseInt(versionId);
            return ResourcePackVersion.of(fixedVersion, fixedVersion);
        } catch (NumberFormatException ignored) {
            try {
                // Version range?
                final String[] ranged = versionId.split("-");
                final int min = Integer.parseInt(ranged[0]);
                final int max = Integer.parseInt(ranged[1]);
                return ResourcePackVersion.of(min, max);
            } catch (NumberFormatException | IndexOutOfBoundsException ignored2) {}
        }

        throw new IllegalArgumentException("Invalid version id: " + versionId);
    }

    default Set<ResourcePack> getPacksForVersion(int protocolVersion) {
        final int packFormat = PackFormatResolver.getPackFormat(protocolVersion);

        log("Searching for a resource pack with pack version " + packFormat);

        ResourcePack anyVersionPack = null;
        Set<ResourcePack> validPacks = new HashSet<>();
        for (ResourcePack resourcePack : getResourcePacks()) {
            final Optional<ResourcePackVersion> version = resourcePack.getVersion();
            log("Trying resource pack " + resourcePack.getURL() + " (" + (version.isEmpty() ? version.toString() : version.get().toString()) + ")");

            if (version.isEmpty()) {
                if (anyVersionPack == null) anyVersionPack = resourcePack; // Pick first all-version resource pack
                validPacks.add(resourcePack); // This is still a valid pack that we want to apply.
                continue;
            }

            if (version.get().inVersion(packFormat)) {
                validPacks.add(resourcePack);
                log("Added resource pack " + resourcePack.getURL());
                if (protocolVersion < 765) { // If < 1.20.3, only one pack can be applied.
                    break;
                }
            }
        }

        if (!validPacks.isEmpty()) {
            log("Found multiple valid resource packs (" + validPacks.size() + ")");
            for (ResourcePack validPack : validPacks) {
                log("Chosen resource pack " + validPack.getURL());
            }
            return validPacks;
        }

        log("Chosen resource pack is " + (anyVersionPack == null ? "null" : anyVersionPack.getURL()));
        return anyVersionPack == null ? Set.of() : Set.of(anyVersionPack);
    }

    void log(String info, Object... format);
}
