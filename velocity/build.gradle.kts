repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":api"))
    implementation(project(":webserver", "shadow"))

    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    compileOnly("com.electronwill.night-config:toml:3.8.0")

    implementation("org.bstats:bstats-velocity:3.0.2")

    implementation("org.incendo:cloud-velocity:2.0.0-beta.10") {
        exclude("org.checkerframework")
    }
}

tasks.shadowJar {
    minimize {
        exclude(project(":webserver"))
    }
    relocate("org.bstats", "com.convallyria.forcepack.velocity.libs.bstats")
}