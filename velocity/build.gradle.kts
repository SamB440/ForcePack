plugins {
    id("com.github.johnrengelman.shadow")
    id("java")
}

repositories {
    maven {
        name = "papermc-repo"
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }
}

dependencies {
    implementation(project(":api"))

    compileOnly("com.velocitypowered:velocity-api:3.1.2-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.0.1-SNAPSHOT")
    compileOnly("com.electronwill.night-config:toml:3.6.4")

    implementation("org.bstats:bstats-velocity:3.0.0")
}

tasks.shadowJar {
    relocate("org.bstats", "com.convallyria.forcepack.velocity.libs.bstats")
}