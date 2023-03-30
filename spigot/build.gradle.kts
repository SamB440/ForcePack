repositories {
    maven("https://erethon.de/repo/")
    maven("https://repo.convallyria.com/releases")
    maven("https://repo.viaversion.com")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://jitpack.io")
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation(project(":api"))
    implementation(project(":folia"))

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("com.viaversion:viaversion-api:4.1.1")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.0.0")

    implementation("com.convallyria.languagy:api:3.0.2") {
        exclude("com.convallyria.languagy.libs")
    }
    implementation("org.bstats:bstats-bukkit:3.0.1")
}

tasks {
    shadowJar {
        minimize()
        relocate("io.papermc.lib", "com.convallyria.forcepack.spigot.libs.paperlib")
        relocate("com.convallyria.languagy", "com.convallyria.forcepack.spigot.libs.languagy")
        relocate("org.bstats", "com.convallyria.forcepack.spigot.libs.bstats")
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }
}