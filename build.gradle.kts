import java.util.concurrent.ConcurrentHashMap

plugins {
    id("com.github.ben-manes.versions")
}

tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version)
    }
    outputFormatter {
        val groups = setOf("io.ktor", "org.apache.kafka")
        val printed = ConcurrentHashMap<String, Boolean>()
        this.outdated.dependencies.forEach { dependency ->
            val group = dependency.group
            val print_project: Boolean
            if (group != null && group in groups) {
                if (printed.putIfAbsent(group, true) != true) {
                    println("- [Group] ${dependency.group} [${dependency.version} -> " +
                            "${dependency.available.release ?: dependency.available.milestone ?: dependency.available.integration}]")
                    printed[group] = true
                    print_project = true
                } else {
                    print_project = false
                }
            } else {
                println("- ${dependency.group}:${dependency.name} [${dependency.version} -> " +
                        "${dependency.available.release ?: dependency.available.milestone ?: dependency.available.integration}]")
                print_project = true
            }
            if (print_project) {
                val url = dependency.projectUrl
                if (url != null) {
                    println("    $url\n")
                }
            }
        }
    }
}

fun isNonStable(version: String): Boolean {
    val unstableKeywords = listOf("alpha", "beta", "rc", "cr", "m", "preview", "snapshot", "dev", "eap")
    val normalized = version.lowercase()
    return unstableKeywords.any { normalized.contains(it) }
}


