plugins {
    id("buildlogic.java-common-conventions")
    id("buildlogic.java-modrinth-conventions")
}

dependencies {
    implementation(project(":velocity", "shadow"))
    implementation(project(":paper", "shadow"))
}

tasks {
    shadowJar {
        archiveClassifier.set("paper-velocity")
        minimize {
            exclude(project(":webserver"))
            exclude(project(":velocity"))
            exclude(project(":paper"))
        }
    }
}

modrinth {
    uploadFile.set(tasks.shadowJar)
    gameVersions.addAll("1.8.8", "1.8.9",
        "1.9", "1.9.1", "1.9.2", "1.9.3", "1.9.4",
        "1.10", "1.10.1", "1.10.2",
        "1.11", "1.11.1", "1.11.2",
        "1.12", "1.12.1", "1.12.2",
        "1.13", "1.13.1", "1.13.2",
        "1.14", "1.14.1", "1.14.2", "1.14.3", "1.14.4",
        "1.15", "1.15.1", "1.15.2",
        "1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5",
        "1.17", "1.17.1",
        "1.18", "1.18.1", "1.18.2",
        "1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4",
        "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
        "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4", "1.21.5", "1.21.6", "1.21.7")
    loaders.addAll("bukkit", "folia", "paper", "velocity")
}
