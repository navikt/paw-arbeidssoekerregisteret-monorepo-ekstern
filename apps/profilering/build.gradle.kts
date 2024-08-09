import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask

plugins {
    kotlin("jvm") version "1.9.20"
    id("io.ktor.plugin") version "2.3.5"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("com.google.cloud.tools.jib") version "3.4.1"
    application
}
val jvmVersion = JavaVersion.VERSION_21
val image: String? by project

val arbeidssokerregisteretVersion = "1.9348086045.48-1"
val navCommonModulesVersion = "3.2024.05.23_05.46-2b29fa343e8e"
val logstashVersion = "7.3"
val logbackVersion = "1.4.12"
val pawUtilsVersion = "24.03.25.14-1"
val pawPdlClientsVersion = "24.07.04.39-1"
val pawAaRegClientVersion = "24.07.04.18-1"
val kafkaStreamsVersion = "3.6.0"
val jacksonVersion = "2.16.1"
val ktorVersion = "2.3.12"

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    schema("no.nav.paw.arbeidssokerregisteret.api:main-avro-schema:$arbeidssokerregisteretVersion")
    implementation("no.nav.paw.arbeidssokerregisteret.api:main-avro-schema:$arbeidssokerregisteretVersion")
    implementation("no.nav.paw:aareg-client:$pawAaRegClientVersion")
    implementation("no.nav.paw:pdl-client:$pawPdlClientsVersion")

    implementation("no.nav.paw.hoplite-config:hoplite-config:$pawUtilsVersion")

    implementation("no.nav.common:token-client:$navCommonModulesVersion")

    //ktor server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")

    //Otel
    implementation("io.opentelemetry:opentelemetry-api:1.39.0")
    implementation("io.opentelemetry.instrumentation:opentelemetry-ktor-2.0:2.4.0-alpha")

    // Micrometer
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.3")

    // Ktor client
    implementation("io.ktor:ktor-server-status-pages:${pawObservability.versions.ktor}")
    implementation("io.ktor:ktor-serialization-jackson:${pawObservability.versions.ktor}")
    implementation("io.ktor:ktor-client-okhttp-jvm:${pawObservability.versions.ktor}")
    implementation("io.ktor:ktor-client-logging-jvm:${pawObservability.versions.ktor}")
    implementation("io.ktor:ktor-client-content-negotiation:${pawObservability.versions.ktor}")

    // Logging
    implementation("no.nav.common:log:$navCommonModulesVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    // Kafka
    implementation("no.nav.paw.kafka:kafka:$pawUtilsVersion")
    implementation("no.nav.paw.kafka-streams:kafka-streams:$pawUtilsVersion")
    implementation("io.confluent:kafka-avro-serializer:7.6.0")
    implementation("io.confluent:kafka-streams-avro-serde:7.6.0")
    implementation("org.apache.avro:avro:1.11.3")
    implementation("org.apache.kafka:kafka-clients:$kafkaStreamsVersion")
    implementation("org.apache.kafka:kafka-streams:$kafkaStreamsVersion")

    // Jackson
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    // kotest
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.8.0")

    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaStreamsVersion")

    // Use the JUnit 5 integration.
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class.java) {
    source(zipTree(schema.singleFile))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
}

tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
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
    to.image = "${image ?: rootProject.name }:${project.version}"
    container {
        environment = mapOf(
            "PROFILERING_APPLICATION_ID" to rootProject.name,
            "PROFILERING_APPLICATION_VERSION" to project.version.toString(),
        )
    }
}
