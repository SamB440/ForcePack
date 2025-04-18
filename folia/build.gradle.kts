plugins {
    id("buildlogic.java-common-conventions")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(project(":api"))

    compileOnly("dev.folia:folia-api:1.19.4-R0.1-SNAPSHOT")
}
