import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask

plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("io.ktor.plugin") version "2.3.5"
    application
}
val logstashVersion = "7.3"
val logbackVersion = "1.4.12"
val pawUtilsVersion = "24.02.06.10-1"
val kafkaStreamsVersion = "3.6.0"

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)

    schema("no.nav.paw.arbeidssokerregisteret.api:arena-avro-schema:1.10-1")
    schema("no.nav.paw.arbeidssokerregisteret.api:main-avro-schema:1.10-1")

    implementation("no.nav.paw.hoplite-config:hoplite-config:$pawUtilsVersion")
    implementation("no.nav.paw.kafka-streams:kafka-streams:$pawUtilsVersion")
    implementation("no.nav.common:log:2.2023.01.10_13.49-81ddc732df3a")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")
    // Kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaStreamsVersion")
    implementation("org.apache.kafka:kafka-streams:$kafkaStreamsVersion")
    implementation("io.confluent:kafka-streams-avro-serde:7.4.0")
    implementation("org.apache.avro:avro:1.11.3")
    implementation("io.prometheus:client_java:1.1.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.12.0")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.6.0")
    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaStreamsVersion")
}

ktor {
    fatJar {
        archiveFileName.set("fat.jar")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssokerregisteret.arena.adapter.AppKt")
}

tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class.java) {
    schema.forEach {
        source(zipTree(it))
    }
}