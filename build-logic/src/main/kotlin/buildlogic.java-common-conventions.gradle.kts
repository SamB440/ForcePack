plugins {
    // Apply the java Plugin to add support for Java.
    java
    id("com.gradleup.shadow")
    id("com.diffplug.spotless")
}

spotless {
    java {
        endWithNewline()
        removeUnusedImports()
//        indentWithTabs(4)
        trimTrailingWhitespace()
        targetExclude("build/generated/**/*")
    }

    kotlinGradle {
        endWithNewline()
        leadingTabsToSpaces(4)
        trimTrailingWhitespace()
    }
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

project.version = "1.3.73"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    disableAutoTargetJvm()
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use JUnit Jupiter test framework
            useJUnitJupiter("5.10.1")
        }
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(11)
    }

    build {
        dependsOn(spotlessApply)
        dependsOn(shadowJar)
    }

    shadowJar {
        // Stupid shadow includes plugin.yml from shaded dependencies like packetevents, overwriting my own.
        // Don't allow this. Also useful for the combined spigot-velocity jar.
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        archiveBaseName.set("forcepack")
        archiveClassifier.set(project.name)
        mergeServiceFiles()
        relocate("me.lucko.jarrelocator", "forcepack.libs.relocator")
        relocate("org.glassfish.jaxb", "forcepack.libs.jaxb")
        relocate("org.objectweb.asm", "forcepack.libs.asm")
        relocate("jakarta.xml.bind", "forcepack.libs.jakarta.xml.bind")
        relocate("jakarta.activation", "forcepack.libs.jakarta.activation")
        relocate("com.sun.xml.txw2", "forcepack.libs.sun.xml")
        relocate("com.sun.istack", "forcepack.libs.sun.istack")
        relocate("com.github.retrooper.packetevents", "forcepack.libs.pe.api")
        relocate("io.github.retrooper.packetevents", "forcepack.libs.pe.impl")
    }
}
