repositories {
    maven {
        name = "opencollab-snapshot"
        url = uri("https://repo.opencollab.dev/maven-snapshots/")
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    // JAX-B dependencies for JDK 9+
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:3.0.1")
    implementation("org.glassfish.jaxb:jaxb-runtime:3.0.2")

    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
}
