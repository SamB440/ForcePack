plugins {
    id("buildlogic.java-common-conventions")
}

dependencies {
    implementation(project(":velocity", "shadow"))
    implementation(project(":spigot", "shadow"))
}

tasks {
    shadowJar {
        minimize {
            exclude(project(":webserver"))
            exclude(project(":velocity"))
            exclude(project(":spigot"))
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
