plugins {
    id("com.github.johnrengelman.shadow")
    id("java")
}

repositories {
    maven {
        name = "velocitypowered-repo"
        url = uri("https://nexus.velocitypowered.com/repository/maven-public/")
    }
}

dependencies {
    implementation(project(":api"))

    compileOnly("com.velocitypowered:velocity-api:3.1.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.0-SNAPSHOT")
    compileOnly("com.electronwill.night-config:toml:3.6.3")

    implementation("net.kyori:adventure-text-minimessage:4.1.0-SNAPSHOT")
    implementation("org.bstats:bstats-velocity:2.2.1")
}

tasks.shadowJar {
    relocate("org.bstats", "com.convallyria.forcepack.velocity.libs.bstats")
}