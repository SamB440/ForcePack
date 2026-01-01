import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    `java-library`
    id("buildlogic.java-platform-conventions")
    id("buildlogic.java-modrinth-conventions")
    id("org.spongepowered.gradle.plugin") version "2.3.0"
}

sponge {
    apiVersion("17.0.0-SNAPSHOT")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0.0-SNAPSHOT")
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
        mavenLocal()
        maven("https://repo.convallyria.com/releases")
        maven("https://repo.convallyria.com/snapshots")
        maven("https://repo.empirewar.org/snapshots")
        maven {
            name = "grimacSnapshots"
            url = uri("https://repo.grim.ac/snapshots")
            content {
                includeGroup("com.github.retrooper")
                includeGroup("ac.grim.grimac")
                includeGroup("ac.grim.packetevents")
            }
        }
        maven("https://repo.codemc.io/repository/maven-snapshots/")
        maven("https://repo.viaversion.com")
    }
}

configurations {
    all {
        resolutionStrategy {
            force("net.kyori:adventure-api:4.25.0")
            force("net.kyori:adventure-bom:4.25.0")
            force("net.kyori:adventure-nbt:4.25.0")
            force("net.kyori:examination:4.25.0")
        }
    }
}

val adventureApiNbt by configurations.creating

dependencies {
    implementation(project(":api"))
    implementation(project(":webserver", "shadow"))
    implementation("com.github.retrooper:packetevents-sponge:2.11.1+4ac4d81-SNAPSHOT")
    implementation("org.bstats:bstats-sponge:3.0.2")
    implementation("org.incendo:cloud-sponge:2.0.0-SNAPSHOT") {
        exclude("org.checkerframework")
        exclude("io.leangen.geantyref")
    }

    // We won't shade the entirety of adventure-api, only two specific classes we need.
    // BECAUSE SOMEONE AT ADVENTURE DECIDED PUTTING THE NBT PACKAGE IN API WAS A GOOD IDEA
    adventureApiNbt("net.kyori:adventure-api:4.25.0")

    compileOnly("io.netty:netty-all:4.1.105.Final")
    compileOnly("com.google.guava:guava:33.4.0-jre")
    compileOnly("com.viaversion:viaversion-api:4.9.2")
}

tasks {
    shadowJar {
        minimize {
            exclude(project(":webserver"))
        }

        // BECAUSE SOMEONE AT ADVENTURE DECIDED PUTTING THE NBT PACKAGE IN API WAS A GOOD IDEA
        from(
            adventureApiNbt.resolve().map { dep ->
                zipTree(dep).matching {
                    include("net/kyori/adventure/nbt/api/**")
                }
            }
        )

        relocate("org.bstats", "forcepack.libs.bstats")
        relocate("net.kyori.adventure.nbt", "forcepack.libs.adventure.nbt")
        relocate("net.kyori.examination", "forcepack.libs.adventure.ex")
    }
}

modrinth {
    uploadFile.set(tasks.shadowJar)
    versionName.set(versionName.get() + " - Sponge")
    gameVersions.addAll("1.21.8", "1.21.9", "1.21.10", "1.21.11") // Must be an array, even with only one version
    loaders.addAll("sponge") // Must also be an array - no need to specify this if you're using Loom or ForgeGradle
}
