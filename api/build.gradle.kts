plugins {
    `maven-publish`
}


repositories {
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")

    // JAX-B dependencies for JDK 9+
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.2")

    compileOnly("org.geysermc.geyser:api:2.4.2-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
}

publishing {
    publications {
        create<MavenPublication>("forcepack") {
            from(components["java"])

            // skip shadow jar from publishing. Workaround for https://github.com/johnrengelman/shadow/issues/651
            val javaComponent = components["java"] as AdhocComponentWithVariants
            javaComponent.withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }
        }
    }

    repositories {
        // See Gradle docs for how to provide credentials to PasswordCredentials
        // https://docs.gradle.org/current/samples/sample_publishing_credentials.html

        maven {
            val isSnapshot = true//ver.contains("SNAPSHOT")
            name = if (isSnapshot) "snapshots" else "releases"
            url = uri("https://repo.convallyria.com/$name/")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}
