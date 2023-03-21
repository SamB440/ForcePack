plugins {
    id("com.github.johnrengelman.shadow") version "7.1.2"
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
        toolchain.languageVersion.set(JavaLanguageVersion.of(11))
    }

    repositories {
        mavenCentral()
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
        }

        build {
            dependsOn(shadowJar)
        }
    }
}
