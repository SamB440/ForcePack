java {
    disableAutoTargetJvm()
}

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    implementation(project(":api"))

    compileOnly("dev.folia:folia-api:1.19.4-R0.1-SNAPSHOT")
}
