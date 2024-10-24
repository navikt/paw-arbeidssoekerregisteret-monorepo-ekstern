rootProject.name = "paw-arbeidssoekerregisteret-monorepo-ekstern"

include(
    "domain:main-avro-schema",
    "lib:hoplite-config",
    "lib:error-handling",
    "lib:kafka",
    "lib:kafka-streams",
    "lib:kafka-key-generator-client",
    "test:test-data-factory",
    "apps:microfrontend-toggler",
    "apps:profilering",
    "apps:oppslag-api",
    "apps:eksternt-api",
)

dependencyResolutionManagement {
    val githubPassword: String by settings
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven {
            url = uri("https://packages.confluent.io/maven/")
        }
        maven {
            setUrl("https://maven.pkg.github.com/navikt/poao-tilgang")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        maven {
            setUrl("https://maven.pkg.github.com/navikt/tms-varsel-authority")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
        maven {
            setUrl("https://maven.pkg.github.com/navikt/paw-arbeidssokerregisteret-api")
            credentials {
                username = "x-access-token"
                password = githubPassword
            }
        }
    }
}
