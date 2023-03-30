repositories {
    maven {
        name = "opencollab-snapshot"
        url = uri("https://repo.opencollab.dev/maven-snapshots/")
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    // JAX-B dependencies for JDK 9+
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.2")

    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
}
