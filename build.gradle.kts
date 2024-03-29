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
    version = "1.3.3"

    apply(plugin = "com.github.johnrengelman.shadow")
    apply(plugin = "java")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
        disableAutoTargetJvm()
    }

    dependencies {
        implementation("org.incendo:cloud-annotations:${properties["cloud_version"]}") {
            exclude("org.checkerframework")
        }
        annotationProcessor("org.incendo:cloud-annotations:${properties["cloud_version"]}")
    }

    repositories {
        mavenCentral()
        maven("https://erethon.de/repo/")
        maven("https://repo.convallyria.com/snapshots")
        maven("https://repo.viaversion.com")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://oss.sonatype.org/content/groups/public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
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
