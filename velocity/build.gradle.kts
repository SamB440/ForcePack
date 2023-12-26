repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")

    maven {
        url = uri("https://repo.codemc.io/repository/maven-releases/")
    }
}

dependencies {
    implementation(project(":api"))
    implementation(project(":webserver", "shadow"))

    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly("com.electronwill.night-config:toml:3.6.6")

    implementation("org.bstats:bstats-velocity:3.0.1")
}

tasks.shadowJar {
    minimize {
        exclude(project(":webserver"))
    }
    relocate("org.bstats", "com.convallyria.forcepack.velocity.libs.bstats")
}