import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.openapitools.generator.gradle.plugin.tasks.ValidateTask

plugins {
    kotlin("jvm")
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
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":lib:serialization"))
    implementation(project(":lib:logging"))
    implementation(project(":lib:health"))

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
    mainClass.set("no.nav.paw.ledigestillinger.ApplicationKt")
}
