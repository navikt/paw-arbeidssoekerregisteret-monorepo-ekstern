import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("io.ktor.plugin") version "2.3.5"
    id("com.google.cloud.tools.jib") version "3.4.0"
    application
}

val jvmVersion = JavaVersion.VERSION_21
val image: String? by project

val arbeidssoekerregisteretApiVersion = "1.8062260419.22-1"
val logstashVersion = "7.3"
val logbackVersion = "1.4.12"
val pawUtilsVersion = "24.02.06.10-1"
val kafkaStreamsVersion = "3.6.0"

val schema by configurations.creating {
    isTransitive = false
}

val agent by configurations.creating {
    isTransitive = false
}

val agentExtension by configurations.creating {
    isTransitive = false
}

val agentExtensionJar = "agent-extension.jar"
val agentJar = "agent.jar"
val agentFolder = layout.buildDirectory.dir("agent").get().toString()
val agentExtensionFolder = layout.buildDirectory.dir("agent-extension").get().toString()


dependencies {
    agent("io.opentelemetry.javaagent:opentelemetry-javaagent:1.31.0")
    agentExtension("no.nav.paw.observability:opentelemetry-anonymisering-1.31.0:23.10.25.8-1")
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)

    schema("no.nav.paw.arbeidssokerregisteret.api:arena-avro-schema:$arbeidssoekerregisteretApiVersion")
    schema("no.nav.paw.arbeidssokerregisteret.api:main-avro-schema:$arbeidssoekerregisteretApiVersion")

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

tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks.named("compileTestKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
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

tasks.create("addAgent", Copy::class) {
    from(agent)
    into(agentFolder)
    rename { _ -> agentJar}
}

tasks.create("addAgentExtension", Copy::class) {
    from(agentExtension)
    into(agentExtensionFolder)
    rename { _ -> agentExtensionJar}
}

tasks.withType(KotlinCompile::class) {
    dependsOn.add("addAgent")
    dependsOn.add("addAgentExtension")
}

jib {
    from.image = "ghcr.io/navikt/baseimages/temurin:${jvmVersion.majorVersion}"
    to.image = "${image ?: rootProject.name }:${project.version}"
    extraDirectories {
        paths {
            path {
                setFrom(agentFolder)
                into = "/app"
            }
            path {
                setFrom(agentExtensionFolder)
                into = "/app"
            }
        }
    }
    container.entrypoint = listOf(
        "java",
        "-cp", "@/app/jib-classpath-file",
        "-javaagent:/app/$agentJar",
        "-Dotel.javaagent.extensions=/app/$agentExtensionJar",
        "-Dotel.resource.attributes=service.name=${project.name}",
        application.mainClass.get()
    )
}