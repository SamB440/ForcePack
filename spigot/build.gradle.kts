repositories {
    maven("https://erethon.de/repo/")
    maven("https://repo.convallyria.com/snapshots")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.viaversion.com")
    maven {
        name = "codemc-repo"
        url = uri("https://repo.codemc.org/repository/maven-snapshots/")
    }
    maven {
        name = "papermc"
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }
}

dependencies {
    implementation(project(":api"))

    compileOnly("org.spigotmc:spigot-api:1.18.1-R0.1-SNAPSHOT")
    compileOnly("com.viaversion:viaversion-api:4.1.1")

    implementation("net.islandearth:languagy:2.0.4-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:3.0.0")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
}

tasks {
    shadowJar {
        relocate("io.papermc.lib", "com.convallyria.forcepack.spigot.libs.paperlib")
        relocate("net.islandearth.languagy", "com.convallyria.forcepack.spigot.libs.languagy")
        relocate("org.bstats", "com.convallyria.forcepack.spigot.libs.bstats")
        relocate("co.aikar.commands", "com.convallyria.forcepack.spigot.libs.acf")
        relocate("co.aikar.locales", "com.convallyria.forcepack.spigot.libs.acf")
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}