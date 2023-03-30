plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
}

dependencies {
    implementation(project(":spigot", "shadow"))
    implementation(project(":velocity", "shadow"))
}

allprojects {
    group = "com.convallyria.forcepack"
    version = "1.2.9"

    apply(plugin = "com.github.johnrengelman.shadow")
    apply(plugin = "java")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    dependencies {
        implementation("cloud.commandframework:cloud-paper:${properties["cloud_version"]}") {
            exclude("org.checkerframework")
        }
        implementation("cloud.commandframework:cloud-annotations:${properties["cloud_version"]}") {
            exclude("org.checkerframework")
        }
        implementation("cloud.commandframework:cloud-velocity:${properties["cloud_version"]}") {
            exclude("org.checkerframework")
        }
    }

    repositories {
        mavenCentral()
        maven("https://erethon.de/repo/")
        maven("https://repo.convallyria.com/snapshots")
        maven("https://repo.viaversion.com")
        maven {
            name = "codemc-repo"
            url = uri("https://repo.codemc.org/repository/maven-snapshots/")
        }
        maven {
            name = "papermc"
            url = uri("https://repo.papermc.io/repository/maven-public/")
        }
        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/content/groups/public/")
        }
    }

    tasks {
        test {
            useJUnitPlatform()

            testLogging {
                events("passed", "skipped", "failed")
            }
        }

        shadowJar {
            archiveClassifier.set("")
            relocate("cloud.commandframework", "com.convallyria.forcepack.libs.commandframework")
            relocate("io.leangen.geantyref", "com.convallyria.forcepack.libs.typetoken")
        }

        build {
            dependsOn(shadowJar)
        }

        compileJava {
            // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
            // See https://openjdk.java.net/jeps/247 for more information.
            options.release.set(11)
        }
    }
}
