import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask

plugins {
    kotlin("jvm") version "1.9.20"
    id("io.ktor.plugin") version "2.3.9"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("org.openapi.generator") version "7.4.0"
    application
}

val arbeidssoekerregisteretSchemaVersion = "1.9348086045.48-1"
val logbackVersion = "1.4.14"
val logstashVersion = "7.4"
val pawUtilsVersion = "24.01.11.9-1"
val navCommonModulesVersion = "2.2023.01.02_13.51-1c6adeb1653b"
val tokenSupportVersion = "4.1.0"
val koTestVersion = "5.8.0"
val hopliteVersion = "2.7.5"
val exposedVersion = "0.53.0"
val poaoVersion = "2024.01.05_08.39-83879ad64bab"
val ktorVersion = pawObservability.versions.ktor

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    // Arbeidssoekerregisteret schema
    schema("no.nav.paw.arbeidssokerregisteret.api:main-avro-schema:$arbeidssoekerregisteretSchemaVersion")

    // OpenTelemetry
    implementation(pawObservability.bundles.ktorNettyOpentelemetryMicrometerPrometheus)
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.1.0")

    // Token support
    implementation("no.nav.security:token-validation-ktor-v2:$tokenSupportVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.common:token-client:$navCommonModulesVersion")

    // Logging
    implementation("no.nav.common:log:$navCommonModulesVersion")
    implementation("no.nav.common:audit-log:$navCommonModulesVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    // Paw-utils
    implementation("no.nav.paw.hoplite-config:hoplite-config:$pawUtilsVersion")
    implementation("no.nav.paw.kafka:kafka:$pawUtilsVersion")

    // Poao-tilgang
    implementation("com.github.navikt.poao-tilgang:client:$poaoVersion")

    // Kafka
    implementation("org.apache.avro:avro:1.11.3")
    implementation("io.confluent:kafka-avro-serializer:7.5.1")

    // Ktor server
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.2")

    // Ktor client
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion}")

    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("org.flywaydb:flyway-core:9.21.2")

    // Test
    implementation("org.mockito:mockito-core:3.12.4")
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$koTestVersion")
    testImplementation("io.kotest:kotest-assertions-core:$koTestVersion")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.testcontainers:testcontainers:1.19.1")
    testImplementation("org.testcontainers:postgresql:1.19.1")
    testImplementation("no.nav.security:mock-oauth2-server:2.0.0")
}
sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

val opneApiDocFile = "${layout.projectDirectory}/src/main/resources/openapi/documentation.yaml"
val generatedCodePackageName = "no.nav.paw.arbeidssoekerregisteret.api.oppslag"
val generatedCodeOutputDir = "${layout.buildDirectory.get()}/generated/"

openApiValidate {
    inputSpec = opneApiDocFile
}

openApiGenerate {
    generatorName.set("kotlin-server")
    library = "ktor"
    inputSpec = opneApiDocFile
    outputDir = generatedCodeOutputDir
    packageName = generatedCodePackageName
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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("no.nav.paw.arbeidssoekerregisteret.api.oppslag.ApplicationKt")
}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class.java) {
    source(zipTree(schema.singleFile))
}

tasks.named("compileTestKotlin") {
    dependsOn("generateTestAvroJava", "openApiValidate", "openApiGenerate")
}

tasks.named("compileKotlin") {
    dependsOn("generateAvroJava", "openApiValidate", "openApiGenerate")
}

task<JavaExec>("produceLocalMessagesForTopics") {
    mainClass.set("no.nav.paw.arbeidssoekerregisteret.api.oppslag.kafka.producers.LocalProducerKt")
    classpath = sourceSets["test"].runtimeClasspath
}

task<JavaExec>("cleanDatabase") {
    mainClass.set("no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.DatabaseUtilsKt")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

ktor {
    fatJar {
        archiveFileName.set("fat.jar")
    }
}
