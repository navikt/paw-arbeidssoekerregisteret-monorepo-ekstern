rootProject.name = "paw-arbeidssoekerregisteret-monorepo-ekstern"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    kotlin("jvm") version "2.3.0" apply false
    kotlin("plugin.serialization") version "2.3.0" apply false
    id("com.google.cloud.tools.jib") version "3.5.2" apply false
    id("org.openapi.generator") version "7.19.0" apply false
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1" apply false
    id("com.expediagroup.graphql") version "8.8.1" apply false
}

include(
    // domain
    "domain:error",
    "domain:felles",
    "domain:main-avro-schema",
    "domain:arena-avro-schema",
    "domain:bekreftelsesmelding-avro-schema",
    "domain:bekreftelse-paavegneav-avro-schema",
    "domain:ledigestillinger",
    "domain:arbeidsplassen-stillinger-feed",
    "domain:arbeidsplassen-stillinger-avro-schema",
    // libs
    "lib:health",
    "lib:logging",
    "lib:serialization",
    "lib:hoplite-config",
    "lib:database",
    "lib:scheduling",
    "lib:error-handling",
    "lib:metrics",
    "lib:api-docs",
    "lib:security",
    "lib:http-client-utils",
    "lib:kafka",
    "lib:kafka-hwm",
    "lib:kafka-streams",
    "lib:kafka-key-generator-client",
    "lib:pdl-client",
    "lib:aareg-client",
    "lib:tilgangskontroll-client",
    "lib:topics",
    // test
    "test:test-data-factory",
    // apps
    "apps:microfrontend-toggler",
    "apps:egenvurdering-api",
    "apps:egenvurdering-dialog-tjeneste",
    "apps:profilering",
    "apps:oppslag-api",
    "apps:eksternt-api",
    "apps:arena-adapter",
    "apps:oppslag-api-v2",
    "apps:mine-stillinger-api",
    "apps:ledigestillinger-api",
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
