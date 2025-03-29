plugins {
    id("io.github.goooler.shadow") version "8.1.8"
    id("java")
    `maven-publish`
}

dependencies {
    implementation(project(":spigot", "shadow"))
    implementation(project(":velocity", "shadow"))
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("org.incendo.cloud", "com.convallyria.forcepack.libs.cloud")
        relocate("io.leangen.geantyref", "com.convallyria.forcepack.libs.typetoken")
        relocate("me.lucko.jarrelocator", "com.convallyria.forcepack.libs.relocator")
    }

    build {
        dependsOn(shadowJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "com.convallyria.forcepack"
            artifactId = "ForcePack"
            version = "1.3.72"
            artifact(file("C:/Users/Tom/IdeaProjects/ForcePack/build/libs/ForcePack-1.3.72.jar"))
        }
    }
    repositories {
        mavenLocal()
    }
}

allprojects {
    group = "com.convallyria.forcepack"
    version = "1.3.72"

    apply(plugin = "io.github.goooler.shadow")
    apply(plugin = "java")

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
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
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }

    tasks {
        test {
            useJUnitPlatform()

            testLogging {
                events("passed", "skipped", "failed")
            }
        }

        compileJava {
            // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
            // See https://openjdk.java.net/jeps/247 for more information.
            options.release.set(11)
        }
    }
}
