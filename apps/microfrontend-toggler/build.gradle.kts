import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    //id("io.ktor.plugin")
    id("org.openapi.generator")
    id("com.google.cloud.tools.jib")
    application
}

val jvmMajorVersion: String by project
val baseImage: String by project
val image: String? by project

val agents by configurations.creating

dependencies {
    // Project
    implementation(project(":lib:app-config"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:rapportering-interne-hendelser"))

    // Server
    implementation(ktorServer.bundles.withNettyAndMicrometer)
    implementation(ktorServer.contentNegotiation)
    implementation(ktorServer.statusPages)
    implementation(ktorServer.cors)
    implementation(ktorServer.cors)
    implementation(ktorServer.callId)
    implementation(ktorServer.auth)

    // Client
    implementation(ktorClient.bundles.withCio)
    implementation(ktorClient.contentNegotiation)

    // Serialization
    implementation(ktor.serializationJackson)
    implementation(ktor.serializationJson)
    implementation(jackson.datatypeJsr310)

    // Logging
    implementation(loggingLibs.logbackClassic)
    implementation(loggingLibs.logstashLogbackEncoder)
    implementation(navCommon.log)
    implementation(navCommon.auditLog)

    // Docs
    implementation(ktorServer.openapi)
    implementation(ktorServer.swagger)

    // Instrumentation
    implementation(micrometer.registryPrometheus)
    implementation(otel.api)
    implementation(otel.annotations)

    // Kafka
    implementation(orgApacheKafka.kafkaStreams)
    implementation(apacheAvro.kafkaStreamsAvroSerde)

    // NAV Common
    implementation(navCommon.types)

    // NAV Security
    implementation(navSecurity.tokenValidationKtorV2)

    // NAV TMS
    implementation(tmsVarsel.kotlinBuilder)

    // Test
    testImplementation(ktorServer.testJvm)
    testImplementation(testLibs.bundles.withUnitTesting)
    testImplementation(testLibs.mockk)
    testImplementation(testLibs.mockOauth2Server)
    testImplementation(orgApacheKafka.streamsTest)

    agents(otel.javaagent)
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
    mainClass.set("no.nav.paw.arbeidssoekerregisteret.ApplicationKt")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Title"] = rootProject.name
    }
}

tasks.register<Copy>("copyAgents") {
    from(agents)
    into("${layout.buildDirectory.get()}/agents")
}

tasks.named("assemble") {
    finalizedBy("copyAgents")
}

jib {
    from.image = "$baseImage:$jvmMajorVersion"
    to.image = "${image ?: project.name}:${project.version}"
    container {
        environment = mapOf(
            "IMAGE_WITH_VERSION" to "${image ?: project.name}:${project.version}",
            "OTEL_INSTRUMENTATION_METHODS_INCLUDE" to ("org.apache.kafka.streams.state.internals.RocksDBStore[put]")
        )
        jvmFlags = listOf(
            "-XX:ActiveProcessorCount=4", "-XX:+UseZGC", "-XX:+ZGenerational"
        )
    }
}
