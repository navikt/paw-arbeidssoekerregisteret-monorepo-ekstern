plugins {
    kotlin("jvm")
    id("jib-distroless")
    application
}

val jvmMajorVersion: String by project

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:arena-avro-schema"))
    implementation(project(":domain:bekreftelsesmelding-avro-schema"))
    implementation(project(":lib:topics"))

    // Jackson
    implementation(libs.jackson.kotlin)

    // Server
    implementation(libs.bundles.ktor.server.instrumented)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.nav.common.log)
    implementation(libs.nav.common.audit.log)

    // Instrumentation
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.annotations)

    // Kafka
    implementation(libs.kafka.streams)
    implementation(libs.avro)
    implementation(libs.confluent.kafka.avro.serializer)
    implementation(libs.confluent.kafka.streams.avro.serde)

    // Test
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kafka.streams.test)
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(project(":test:test-data-factory"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.arena.adapter.AppKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
