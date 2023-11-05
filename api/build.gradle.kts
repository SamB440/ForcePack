repositories {
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")

    // JAX-B dependencies for JDK 9+
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.2")

    compileOnly("org.geysermc.geyser:api:2.2.0-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
}
