import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.openapi.generator")
    id("com.google.cloud.tools.jib")
    application
}

val jvmMajorVersion: String by project
val baseImage: String by project
val image: String? by project

dependencies {
    // Project
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka-streams"))

    // Server
    implementation(ktorServer.bundles.withNettyAndMicrometer)
    implementation(ktorServer.contentNegotiation)
    implementation(ktorServer.statusPages)
    implementation(ktorServer.cors)
    implementation(ktorServer.callId)
    implementation(ktorServer.auth)

    // Serialization
    implementation(ktor.serializationJackson)
    implementation(ktor.serializationJson)
    implementation(jackson.datatypeJsr310)

    // Authentication
    implementation(navSecurity.tokenValidationKtorV2)

    // Documentation
    implementation(ktorServer.swagger)

    // Logging
    implementation(loggingLibs.logbackClassic)
    implementation(loggingLibs.logstashLogbackEncoder)
    implementation(navCommon.log)

    // Instrumentation
    implementation(micrometer.registryPrometheus)
    implementation(otel.ktor)
    implementation(otel.api)

    // Kafka
    implementation(orgApacheKafka.kafkaStreams)
    implementation(apacheAvro.kafkaStreamsAvroSerde)

    // Test
    testImplementation(ktorServer.testJvm)
    testImplementation(testLibs.bundles.withUnitTesting)
}

sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

application {
    mainClass.set("no.nav.paw.rapportering.api.ApplicationKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Title"] = rootProject.name
    }
}

jib {
    from.image = "$baseImage:$jvmMajorVersion"
    to.image = "${image ?: project.name}:${project.version}"
    container {
        environment = mapOf(
            "IMAGE_WITH_VERSION" to "${image ?: project.name}:${project.version}"
        )
        jvmFlags = listOf("-XX:ActiveProcessorCount=4", "-XX:+UseZGC", "-XX:+ZGenerational")
    }
}
