rootProject.name = "paw-arbeidssoekerregisteret-monorepo-ekstern"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    kotlin("jvm") version "2.1.20" apply false
    kotlin("plugin.serialization") version "2.1.20" apply false
    id("com.google.cloud.tools.jib") version "3.4.5" apply false
    id("org.openapi.generator") version "7.12.0" apply false
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1" apply false
    id("com.expediagroup.graphql") version "8.4.0" apply false
}

include(
    // domain
    "domain:main-avro-schema",
    "domain:arena-avro-schema",
    "domain:bekreftelsesmelding-avro-schema",
    // libs
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
    "lib:kafka-streams",
    "lib:kafka-key-generator-client",
    "lib:pdl-client",
    "lib:aareg-client",
    "lib:tilgangskontroll-client",
    "lib:common-model",
    // test
    "test:test-data-factory",
    // apps
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
