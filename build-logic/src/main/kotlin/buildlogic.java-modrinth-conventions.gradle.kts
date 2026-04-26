plugins {
    id("com.modrinth.minotaur")
}

// Helper methods
fun executeGitCommand(vararg command: String): String {
    return providers.exec {
        commandLine = listOf("git", *command)
    }.standardOutput.asText.get().trim()
}

fun latestCommitChangelog(): String {
    val shortHash = executeGitCommand("log", "-1", "--pretty=format:%h")
    val fullHash = executeGitCommand("log", "-1", "--pretty=format:%H")
    val author = executeGitCommand("log", "-1", "--pretty=format:%an")
    val date = executeGitCommand("log", "-1", "--pretty=format:%ad", "--date=short")
    val subject = executeGitCommand("log", "-1", "--pretty=format:%s")
    val body = executeGitCommand("log", "-1", "--pretty=format:%b")

    return buildString {
        appendLine("### Commit [`$shortHash`](https://github.com/SamB440/ForcePack/commit/$fullHash)")
        appendLine()
        appendLine("**$subject**")
        if (body.isNotBlank()) {
            appendLine()
            appendLine(body.trim())
        }
        appendLine()
        append("by $author on $date")
    }
}

fun getChangelogSinceLastRelease(): String {
    // Get the latest tag (assuming tags follow semantic versioning)
    val latestTag = executeGitCommand("describe", "--tags", "--abbrev=0")
    val currentVersion = project.version.toString()

    // Get all commits between the latest tag and HEAD, including short hash links
    val commits = executeGitCommand(
        "log",
        "--pretty=format:- %s ([`%h`](https://github.com/SamB440/ForcePack/commit/%H))",
        "$latestTag..HEAD"
    )
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
    try {
        val changelogContent: String = if (isRelease) {
            getChangelogSinceLastRelease()
        } else {
            latestCommitChangelog()
        }
        changelog.set(changelogContent)
    } catch (e: Exception) {
        println("Failed to generate changelog: ${e.message}. Was the repository cloned with tags?")
        changelog.set("Failed to generate changelog: ${e.message}")
    }
}
