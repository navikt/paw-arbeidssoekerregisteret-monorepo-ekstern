import java.util.concurrent.ConcurrentHashMap

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

// Transitive avhengigheter løftes her for å lukke kjente CVE-er.
// Jackson 2 kommer fra Avro, Kafka Streams og Confluent. Jackson 3 kommer fra Ktor.
val jackson2Bom = libs.jackson2.bom
val jackson3Bom = libs.jackson3.bom
subprojects {
    plugins.withId("java") {
        dependencies {
            "implementation"(platform(jackson2Bom))
            "implementation"(platform(jackson3Bom))
            constraints {
                "implementation"("com.google.guava:guava:33.8.0-jre") {
                    because("CVE-2026-102554, Confluent 8.4.1 drar inn 32.0.1-jre")
                }
                "implementation"("org.xerial.snappy:snappy-java:1.1.10.9") {
                    because("CVE-2026-93451, CVE-2026-108106, CVE-2026-116590, fra kafka-clients")
                }
                "implementation"("at.yawk.lz4:lz4-java:1.11.4") {
                    because("CVE-2026-106449, CVE-2026-106450, CVE-2026-106451, fra kafka-clients")
                }
            }
        }
    }
}
