repositories {
    maven("https://erethon.de/repo/")
    maven("https://repo.convallyria.com/releases")
    maven("https://repo.convallyria.com/snapshots")
    maven("https://repo.viaversion.com")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.LoneDev6")
        }
    }
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
}

configurations {
    all {
        resolutionStrategy {
            force("net.kyori:adventure-api:4.17.0")
            force("net.kyori:adventure-bom:4.17.0")
        }
    }
}

dependencies {
    implementation(project(":api"))
    implementation(project(":folia"))
    implementation(project(":webserver", "shadow"))

    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT") {
        exclude("net.kyori") // avoid conflicts
    }
    compileOnly("com.viaversion:viaversion-api:4.9.2")
    compileOnly("io.netty:netty-all:4.1.105.Final")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1")

    implementation("org.incendo:cloud-paper:2.0.0-beta.10") {
        exclude("org.checkerframework")
    }
    implementation("net.kyori:adventure-platform-bukkit:4.3.4") {
        exclude("net.kyori", "adventure-api") // not up-to-date - use minimessage version
    }
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("com.convallyria.languagy:api:3.0.3-SNAPSHOT") {
        exclude("com.convallyria.languagy.libs")
    }
    implementation("org.bstats:bstats-bukkit:3.0.2")
    implementation("com.github.GrimAnticheat.PacketEvents:packetevents-spigot:2.0-SNAPSHOT")
}

tasks {
    shadowJar {
        minimize {
            exclude(project(":webserver"))
        }
        relocate("net.kyori", "com.convallyria.forcepack.spigot.libs.adventure")
        relocate("io.papermc.lib", "com.convallyria.forcepack.spigot.libs.paperlib")
        relocate("com.convallyria.languagy", "com.convallyria.forcepack.spigot.libs.languagy")
        relocate("org.bstats", "com.convallyria.forcepack.spigot.libs.bstats")
        relocate("com.github.retrooper.packetevents", "com.convallyria.forcepack.spigot.libs.pe.api")
        relocate("io.github.retrooper.packetevents", "com.convallyria.forcepack.spigot.libs.pe.impl")
//        exclude("**/assets/mappings/") // We don't need these TODO pls fix Krishna
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}