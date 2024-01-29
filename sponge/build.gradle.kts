import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    `java-library`
    id("org.spongepowered.gradle.plugin") version "2.1.1"
}

sponge {
    apiVersion("11.0.0-SNAPSHOT")
    license("GPL-3.0")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.3.3")
    }

    plugin("forcepack") {
        displayName("ForcePack")
        entrypoint("com.convallyria.forcepack.sponge.ForcePackSponge")
        links {
            homepage("https://github.com/SamB440/ForcePack")
            source("https://github.com/SamB440/ForcePack")
            issues("https://github.com/SamB440/ForcePack/issues")
        }

        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}

dependencies {
    implementation(project(":api"))
    implementation(project(":folia"))
    implementation(project(":webserver", "shadow"))
}

// Make sure all tasks which produce archives (jar, sources jar, javadoc jar, etc) produce more consistent output
tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}