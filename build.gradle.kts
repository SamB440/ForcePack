plugins {
    // Load the Spotless plugin at the root project's classloader scope so that
    // the SpotlessTaskService build service is shared across sibling subprojects.
    // Without this, applying Spotless via the buildlogic convention plugin in
    // multiple subprojects loads SpotlessTaskService in separate classloader
    // scopes, causing a "Cannot set the value of task ':*:spotlessJava' property
    // 'taskService'" error when Gradle tries to wire the shared service.
    id("com.diffplug.spotless") version "8.4.0" apply false
}
