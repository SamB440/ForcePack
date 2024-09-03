plugins {
    id("dev.vankka.dependencydownload.plugin") version "1.3.1"
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))
    compileOnly("com.google.guava:guava:32.1.3-jre")

    runtimeDownload("io.javalin:javalin:6.3.0")

    implementation("dev.vankka:dependencydownload-runtime:1.3.1")
}

tasks {
    shadowJar {
        dependsOn(generateRuntimeDownloadResourceForRuntimeDownloadOnly, generateRuntimeDownloadResourceForRuntimeDownload)
        relocate("io.javalin", "com.convallyria.forcepack.libs.javalin")
        generateRuntimeDownloadResourceForRuntimeDownloadOnly.get().relocate("io.javalin", "com.convallyria.forcepack.libs.javalin")
    }

    jar {
        dependsOn(generateRuntimeDownloadResourceForRuntimeDownloadOnly, generateRuntimeDownloadResourceForRuntimeDownload)
    }
}
