repositories {
    maven("https://erethon.de/repo/")
    maven("https://repo.convallyria.com/releases")
    maven("https://repo.viaversion.com")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":api"))
    implementation(project(":folia"))
    implementation(project(":webserver", "shadow"))

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT") {
        exclude("net.kyori") // avoid conflicts
    }
    compileOnly("com.viaversion:viaversion-api:4.9.2")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.0.0")

    implementation("net.kyori:adventure-platform-bukkit:4.3.1") {
        exclude("net.kyori", "adventure-api") // not up-to-date - use minimessage version
    }
    implementation("net.kyori:adventure-text-minimessage:4.15.0")
    implementation("com.convallyria.languagy:api:3.0.3-SNAPSHOT") {
        exclude("com.convallyria.languagy.libs")
    }
    implementation("org.bstats:bstats-bukkit:3.0.1")
    implementation("com.github.retrooper.packetevents:spigot:2.2.0") {
        exclude("net.kyori") // avoid conflicts
        exclude("assets.mappings")
    }
}

tasks {
    shadowJar {
        minimize {
            exclude(project(":webserver"))
        }
        relocate("io.papermc.lib", "com.convallyria.forcepack.spigot.libs.paperlib")
        relocate("com.convallyria.languagy", "com.convallyria.forcepack.spigot.libs.languagy")
        relocate("org.bstats", "com.convallyria.forcepack.spigot.libs.bstats")
        relocate("com.github.retrooper.packetevents", "com.convallyria.forcepack.spigot.libs.pe-api")
        relocate("io.github.retrooper.packetevents", "com.convallyria.forcepack.spigot.libs.pe-impl")
        exclude("**/assets/mappings/") // We don't need these
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}