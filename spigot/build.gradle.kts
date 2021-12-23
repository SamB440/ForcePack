plugins {
    id("com.github.johnrengelman.shadow")
    id("java")
}

repositories {
    maven { url = uri("https://erethon.de/repo/") }
    maven { url = uri("https://repo.convallyria.com/snapshots") }
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

    implementation("io.papermc:paperlib:1.0.7")
    implementation("net.islandearth:languagy:2.0.4-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:2.2.1")
}

tasks.shadowJar {
    relocate("io.papermc.lib", "com.convallyria.forcepack.spigot.libs.paperlib")
    relocate("net.islandearth.languagy", "com.convallyria.forcepack.spigot.libs.languagy")
    relocate("org.bstats", "com.convallyria.forcepack.spigot.libs.bstats")
}