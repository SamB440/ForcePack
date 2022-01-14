plugins {
    id("com.github.johnrengelman.shadow") version("7.0.0")
    id("java")
}

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

dependencies {
    implementation(project(":spigot"))
    implementation(project(":velocity"))
}

allprojects {
    group = "com.convallyria.forcepack"
    version = "1.1.6"

    apply(plugin = "com.github.johnrengelman.shadow")
    apply(plugin = "java")

    repositories {
        mavenCentral()
        mavenLocal()
        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/content/groups/public/")
        }
    }

    dependencies {

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

        processResources {
            filesMatching("plugin.yml") {
                expand("version" to project.version)
            }
        }
    }
}