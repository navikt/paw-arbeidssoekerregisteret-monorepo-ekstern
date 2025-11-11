plugins {
    kotlin("jvm")
    id("jib-chainguard")
    id("org.openapi.generator")
    application
}

val jvmMajorVersion: String by project

dependencies {
    implementation(project(":domain:felles"))
    implementation(project(":domain:error"))
    implementation(project(":domain:ledigestillinger"))
    implementation(project(":domain:arbeidsplassen-stillinger-avro-schema"))
    implementation(project(":lib:logging"))
    implementation(project(":lib:metrics"))
    implementation(project(":lib:health"))
    implementation(project(":lib:api-docs"))
    implementation(project(":lib:database"))
    implementation(project(":lib:security"))
    implementation(project(":lib:serialization"))
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:topics"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:kafka-hwm"))
    implementation(project(":lib:scheduling"))

    // Server
    implementation(libs.bundles.ktor.server.instrumented)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.callid)
    implementation(libs.ktor.server.call.logging)

    // Serialization
    implementation(libs.ktor.serialization.jackson)
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

    // Database
    implementation(libs.exposed.jdbc.v1)
    implementation(libs.exposed.java.time.v1)
    implementation(libs.hikari.connection.pool)
    implementation(libs.postgres.driver)
    implementation(libs.flyway.postgres)

    // Security
    implementation(libs.ktor.server.auth)
    implementation(libs.nav.common.token.client)
    implementation(libs.nav.security.token.client.core)
    implementation(libs.nav.security.token.validation.ktor)

    // Kafka
    implementation(libs.avro)
    implementation(libs.confluent.kafka.avro.serializer)
    implementation(libs.confluent.kafka.streams.avro.serde)

    // Test
    testImplementation(project(":test:test-data-factory"))
    testImplementation(libs.atlassian.oai.swaggerRequestValidator.core)
    testImplementation(libs.ktor.server.test.host)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.nav.security.mock.oauth2.server)
    testImplementation(libs.testcontainers.postgresql)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.ledigestillinger.ApplicationKt")
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

tasks.named("compileTestKotlin") {
    dependsOn("openApiValidate")
}

tasks.named("compileKotlin") {
    dependsOn("openApiValidate")
}

val openApiDocFile = "${layout.projectDirectory}/src/main/resources/openapi/documentation.yaml"

openApiValidate {
    inputSpec = openApiDocFile
}
