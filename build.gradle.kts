plugins {
    id("com.github.ben-manes.versions")
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
}

fun isNonStable(version: String): Boolean {
    val unstableKeywords = listOf("alpha", "beta", "rc", "cr", "m", "preview", "snapshot", "dev", "eap")
    val normalized = version.lowercase()
    return unstableKeywords.any { normalized.contains(it) }
}
