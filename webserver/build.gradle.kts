repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))
    compileOnly("com.google.guava:guava:32.1.3-jre")

    implementation("io.javalin:javalin:5.6.1")
}

tasks {
    shadowJar {
//        minimize {
//            exclude(dependency("org.eclipse.jetty:.*:.*"))
//        }
//        relocate("io.javalin", "com.convallyria.forcepack.libs.javalin")
    }
}