import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    `java-library`
    id("buildlogic.java-platform-conventions")
    id("org.spongepowered.gradle.plugin") version "2.2.0"
}

sponge {
    apiVersion("12.0.0-SNAPSHOT")
    license("GPL-3.0")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.3.71")
    }

    plugin("forcepack") {
        displayName("ForcePack")
        description("Resource pack handling utilities and enforcement, with Velocity and multiple resource packs support. ")
        entrypoint("com.convallyria.forcepack.sponge.ForcePackSponge")
        links {
            homepage("https://github.com/SamB440/ForcePack")
            source("https://github.com/SamB440/ForcePack")
            issues("https://github.com/SamB440/ForcePack/issues")
        }
        license("GPL-3")

        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}

repositories {
    repositories {
        maven("https://repo.convallyria.com/releases")
        maven("https://repo.convallyria.com/snapshots")
        maven("https://repo.codemc.io/repository/maven-snapshots/")
        maven("https://repo.viaversion.com")
    }
}

dependencies {
    implementation(project(":api"))
    implementation(project(":webserver", "shadow"))
    // TODO use grim fork
    implementation("com.github.retrooper:packetevents-sponge:2.8.0-SNAPSHOT")
    implementation("org.bstats:bstats-sponge:3.0.2")
    implementation("org.incendo:cloud-sponge:2.0.0-SNAPSHOT") {
        exclude("org.checkerframework")
        exclude("io.leangen.geantyref")
    }

    compileOnly("com.google.guava:guava:33.4.0-jre")
    compileOnly("com.viaversion:viaversion-api:4.9.2")
}

tasks {
    shadowJar {
        minimize {
            exclude(project(":webserver"))
        }

        relocate("org.bstats", "forcepack.libs.bstats")
        relocate("net.kyori.adventure.nbt", "forcepack.libs.adventure.nbt")
        relocate("net.kyori.examination", "forcepack.libs.adventure.ex")
    }
}

// Make sure all tasks which produce archives (jar, sources jar, javadoc jar, etc) produce more consistent output
tasks.withType(AbstractArchiveTask::class).configureEach {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false
}
