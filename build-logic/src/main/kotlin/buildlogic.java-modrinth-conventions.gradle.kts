import java.io.ByteArrayOutputStream

plugins {
    id("com.modrinth.minotaur")
}

// Helper methods
fun executeGitCommand(vararg command: String): String {
    val byteOut = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", *command)
        standardOutput = byteOut
    }
    return byteOut.toString(Charsets.UTF_8.name()).trim()
}

fun latestCommitMessage(): String {
    val commits = executeGitCommand("log", "--since=24 hours ago", "--pretty=format:- %s")
    return if (commits.isNotBlank()) {
        "Recent changes (last 24 hours):\n$commits"
    } else {
        "No commits in the last 24 hours"
    }
}

fun getChangelogSinceLastRelease(): String {
    // Get the latest tag (assuming tags follow semantic versioning)
    val latestTag = executeGitCommand("describe", "--tags", "--abbrev=0")
    val currentVersion = project.version.toString()

    // Get all commits between the latest tag and HEAD
    val commits = executeGitCommand("log", "--pretty=format:- %s", "$latestTag..HEAD")
        .lines()
        .filter { it.isNotBlank() }
        .joinToString("\n")

    return if (commits.isNotEmpty()) {
        "$commits\n\n**Full Changelog:** https://github.com/SamB440/ForcePack/compare/$latestTag...$currentVersion"
    } else {
        "No changes since last release"
    }
}

modrinth {
    token.set(System.getenv("MODRINTH_TOKEN") ?: "")
    debugMode.set(System.getenv("MODRINTH_TOKEN") == null)
    projectId.set("forcepack") // This can be the project ID or the slug. Either will work!

    val versionString: String = version as String
    val isRelease: Boolean = !versionString.endsWith("-SNAPSHOT")
    val suffixedVersion: String = if (isRelease) {
        versionString
    } else {
        // Give the version a unique name by using the GitHub Actions run number
        versionString + "+" + System.getenv("GITHUB_RUN_NUMBER")
    }

    versionType.set(if (isRelease) "release" else "beta")
    versionName.set("ForcePack $suffixedVersion")
    versionNumber.set(suffixedVersion)

    // Use the commit description for the changelog
    val changelogContent: String = if (isRelease) {
        getChangelogSinceLastRelease()
    } else {
        latestCommitMessage()
    }
    changelog.set(changelogContent)
}