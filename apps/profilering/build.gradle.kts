plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.avro)
    alias(libs.plugins.google.jib)
    application
}
val jvmVersion = JavaVersion.VERSION_21
val image: String? by project

dependencies {
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:pdl-client"))
    implementation(project(":lib:aareg-client"))

    // NAV
    implementation(libs.nav.common.token.client)

    // Ktor
    implementation(libs.ktor.serialization.jackson)
    // Ktor Server
    implementation(libs.bundles.ktor.server.instrumented)
    implementation(libs.ktor.server.status.pages)
    // Ktor Client
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)

    // Logging
    implementation(libs.nav.common.log)
    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)

    // Instrumentation
    implementation(libs.micrometer.registry.prometheus)

    // Kafka
    implementation(libs.confluent.kafka.avro.serializer)
    implementation(libs.confluent.kafka.streams.avro.serde)
    implementation(libs.avro)
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams)

    // Jackson
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.kotlin)

    // Test
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.kafka.streams.test)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.profilering.StartupKt")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Implementation-Title"] = rootProject.name
        attributes["Main-Class"] = application.mainClass.get()
    }
}

jib {
    from.image = "ghcr.io/navikt/baseimages/temurin:${jvmVersion.majorVersion}"
    to.image = "${image ?: rootProject.name}:${project.version}"
    container {
        environment = mapOf(
            "PROFILERING_APPLICATION_ID" to rootProject.name,
            "PROFILERING_APPLICATION_VERSION" to project.version.toString(),
        )
    }
}
