package com.convallyria.forcepack.api;

import com.convallyria.forcepack.api.resourcepack.ResourcePack;
import com.convallyria.forcepack.api.schedule.PlatformScheduler;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ForcePackAPI {

    /**
     * Gets the loaded {@link ResourcePack}s.
     *   These are the resource packs loaded after checks have been completed by the plugin
     *   to verify the SHA-1 hash of the provided URL download.
     * @return the loaded ResourcePacks
     */
    Set<ResourcePack> getResourcePacks();

    /**
     * Gets the scheduler used for the current platform.
     * @return the scheduler for this server platform
     */
    PlatformScheduler<?> getScheduler();

    /**
     * @param uuid The UUID of the player to exempt the next resource pack send from.
     * @return true if the player was successfully exempted, false if they were already on the exemption list.
     */
    boolean exemptNextResourcePackSend(UUID uuid);

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
}
