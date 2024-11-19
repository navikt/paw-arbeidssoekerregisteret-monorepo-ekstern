rootProject.name = "paw-arbeidssoekerregisteret-monorepo-ekstern"

include(
    "domain:main-avro-schema",
    "domain:arena-avro-schema",
    "domain:bekreftelsesmelding-avro-schema",
    "lib:hoplite-config",
    "lib:error-handling",
    "lib:security",
    "lib:http-client-utils",
    "lib:kafka",
    "lib:kafka-streams",
    "lib:kafka-key-generator-client",
    "lib:pdl-client",
    "lib:aareg-client",
    "lib:aareg-client-v2",
    "test:test-data-factory",
    "apps:microfrontend-toggler",
    "apps:profilering",
    "apps:oppslag-api",
    "apps:eksternt-api",
    "apps:arena-adapter",
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
