plugins {
    kotlin("jvm")
    id("org.openapi.generator")
    id("jib-distroless")
    application
}

val jvmMajorVersion: String by project

dependencies {
    // Project
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:security"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":lib:common-model"))
    implementation(project(":lib:health"))
    implementation(project(":domain:main-avro-schema"))
    testImplementation(project(":test:test-data-factory"))

    // Server
    implementation(libs.bundles.ktor.server.instrumented)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.callid)
    implementation(libs.ktor.server.auth)
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

    // Docs
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)

    // Instrumentation
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.annotations)

    // Kafka
    implementation(libs.kafka.streams)
    implementation(libs.confluent.kafka.streams.avro.serde)

    // NAV Common
    implementation(libs.nav.common.types)

    // NAV Security
    implementation(libs.nav.security.token.validation.ktor)

    // NAV TMS
    implementation(libs.nav.tms.varsel.kotlin.builder)

    // Test
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.mockk)
    testImplementation(libs.nav.security.mock.oauth2.server)
    testImplementation(libs.kafka.streams.test)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssoekerregisteret.ApplicationKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Title"] = rootProject.name
    }
}
