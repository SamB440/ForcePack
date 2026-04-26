pluginManagement {
    // Include 'plugins build' to define convention plugins.
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        maven("https://repo.spongepowered.org/repository/maven-public/") // Sponge
    }
}

rootProject.name = "ForcePack"
include("paper")
include("sponge")
include("velocity")
include("api")
include("folia")
include("webserver")
include("combined")