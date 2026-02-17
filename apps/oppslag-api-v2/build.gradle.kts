import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.openapitools.generator.gradle.plugin.tasks.ValidateTask

plugins {
    kotlin("jvm")
    id("org.openapi.generator")
    id("jib-chainguard")
    application
}

val jvmMajorVersion: String by project

dependencies {
    implementation(project(":lib:topics"))
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:database"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:security"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:http-client-utils"))
    implementation(project(":lib:tilgangskontroll-client"))
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":domain:bekreftelse-paavegneav-avro-schema"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:bekreftelsesmelding-avro-schema"))
    implementation(project(":lib:serialization"))
    implementation(project(":lib:logging"))

    // Server
    implementation(libs.bundles.ktor.server.instrumented)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.callid)
    implementation(libs.ktor.server.call.logging)

    // Client
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    // Serialization
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.jackson.datatype.jsr310)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.nav.common.log)
    implementation(libs.nav.common.audit.log)
    implementation(libs.janino)

    // Docs
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)

    // Instrumentation
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.annotations)

    // Kafka
    implementation(libs.avro)
    implementation(libs.confluent.kafka.avro.serializer)
    implementation(libs.confluent.kafka.streams.avro.serde)

    // Database
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.hikari.connection.pool)
    implementation(libs.postgres.driver)
    implementation(libs.flyway.postgres)

    // Security
    implementation(libs.ktor.server.auth)
    implementation(libs.nav.common.token.client)
    implementation(libs.nav.security.token.client.core)
    implementation(libs.nav.security.token.validation.ktor)

    // Test
    testImplementation(libs.atlassian.oai.swaggerRequestValidator.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.nav.security.mock.oauth2.server)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(project(":test:test-data-factory"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.oppslagapi.ApplicationKt")
}

sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

data class ApiSpec(
    val name: String
) {
    val validator: String get() = "${name}ApiValidator"
    val generator: String get() = "${name}ApiGenerator"
    val file: String get() = "${layout.projectDirectory}/src/main/resources/openapi/${name}-spec.yaml"
}

val v1ApiSpec = ApiSpec(name = "v1")
val v2ApiSpec = ApiSpec(name = "v2")
val v3ApiSpec = ApiSpec(name = "v3")
val apiValidatorList = listOf(v1ApiSpec, v2ApiSpec, v3ApiSpec).map { it.validator }
val apiGeneratorList = listOf(v1ApiSpec).map { it.generator }

tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
    }

    named("compileTestKotlin") {
        dependsOn(apiValidatorList + apiGeneratorList)
    }

    named("compileKotlin") {
        dependsOn(apiValidatorList + apiGeneratorList)
    }

    withType(Jar::class) {
        manifest {
            attributes["Implementation-Version"] = project.version
            attributes["Main-Class"] = application.mainClass.get()
            attributes["Implementation-Title"] = rootProject.name
        }
    }

    register<ValidateTask>(v1ApiSpec.validator) {
        inputSpec = v1ApiSpec.file
    }
    register<ValidateTask>(v2ApiSpec.validator) {
        inputSpec = v2ApiSpec.file
    }
    register<ValidateTask>(v3ApiSpec.validator) {
        inputSpec = v3ApiSpec.file
    }

    register<GenerateTask>(v1ApiSpec.generator) {
        generatorName = "kotlin"
        inputSpec = v1ApiSpec.file
        outputDir = "${layout.buildDirectory.get()}/generated/"
        packageName = "no.nav.paw.arbeidssoekerregisteret.api.${v1ApiSpec.name}.oppslag"
        configOptions.set(
            mapOf(
                "serializationLibrary" to "jackson",
                "enumPropertyNaming" to "original",
            ),
        )
        typeMappings = mapOf(
            "DateTime" to "Instant"
        )
        globalProperties = mapOf(
            "apis" to "none",
            "models" to ""
        )
        importMappings = mapOf(
            "Instant" to "java.time.Instant"
        )
    }
}
