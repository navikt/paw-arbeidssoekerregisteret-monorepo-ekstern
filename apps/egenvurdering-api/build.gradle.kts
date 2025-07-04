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
    implementation(project(":lib:kafka-key-generator-client"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:common-model"))
    implementation(project(":lib:http-client-utils"))
    implementation(project(":lib:api-oppslag-client"))
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
    implementation(libs.ktor.client.mock)


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
    implementation(libs.kafka.clients)
    implementation(libs.confluent.kafka.avro.serializer)

    // NAV Common
    implementation(libs.nav.common.types)

    // NAV Security
    implementation(libs.nav.security.token.validation.ktor)

    // NAV TMS
    implementation(libs.nav.tms.varsel.kotlin.builder)

    // Test
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.nav.security.mock.oauth2.server)
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

tasks.named("compileKotlin") {
    dependsOn("openApiValidate", "openApiGenerate")
}

tasks.named("compileTestKotlin") {
    dependsOn("openApiValidate", "openApiGenerate")
}

sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

val openApiDocFile = "${layout.projectDirectory}/src/main/resources/openapi/documentation.yaml"

openApiValidate {
    inputSpec = openApiDocFile
}

openApiGenerate {
    generatorName.set("kotlin-server")
    library = "ktor"
    inputSpec = openApiDocFile
    outputDir = "${layout.buildDirectory.get()}/generated/"
    packageName = "no.nav.paw.arbeidssoekerregisteret.egenvurdering.api"
    configOptions.set(
        mapOf(
            "serializationLibrary" to "jackson",
            "enumPropertyNaming" to "original",
        ),
    )
    typeMappings = mapOf(
        "DateTime" to "LocalDateTime",
    )
    globalProperties = mapOf(
        "apis" to "none",
        "models" to ""
    )
    importMappings = mapOf(
        "LocalDateTime" to "java.time.LocalDateTime"
    )
}