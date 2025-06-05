plugins {
    kotlin("jvm")
    id("org.openapi.generator")
    id("jib-distroless")
    application
}

val jvmMajorVersion: String by project

dependencies {
    implementation(project(":lib:topics"))
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:database"))
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:security"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:http-client-utils"))
    implementation(project(":lib:tilgangskontroll-client"))
    implementation(project(":domain:bekreftelse-paavegneav-avro-schema"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:bekreftelsesmelding-avro-schema"))

    // Server
    implementation(libs.bundles.ktor.server.instrumented)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.callid)
    implementation(libs.ktor.server.call.logging)

    // Client
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    // Serialization
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.serialization.kotlinx.json)
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

    // Kafka
    implementation(libs.avro)
    implementation(libs.confluent.kafka.avro.serializer)
    implementation(libs.confluent.kafka.streams.avro.serde)

    // Database
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.hikari.connection.pool)
    implementation(libs.postgres.driver)
    implementation(libs.flyway.postgres)

    // Security
    implementation(libs.ktor.server.auth)
    implementation(libs.nav.common.token.client)
    implementation(libs.nav.security.token.client.core)
    implementation(libs.nav.security.token.validation.ktor)

    // Test
    testImplementation(libs.ktor.server.test.host)
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
    mainClass.set("no.nav.paw.oppslagapi.ApplicationKt")
}

sourceSets {
    main {
        kotlin {
            srcDir("${layout.buildDirectory.get()}/generated/src/main/kotlin")
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named("compileTestKotlin") {
    dependsOn("openApiValidate", "openApiGenerate")
}

tasks.named("compileKotlin") {
    dependsOn("openApiValidate", "openApiGenerate")
}

tasks.withType(Jar::class) {
    manifest {
        attributes["Implementation-Version"] = project.version
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Title"] = rootProject.name
    }
}

val openApiDocFile = "${layout.projectDirectory}/src/main/resources/openapi/openapi-spec.yaml"

openApiValidate {
    inputSpec = openApiDocFile
}

openApiGenerate {
    generatorName.set("kotlin-server")
    library = "ktor"
    inputSpec = openApiDocFile
    outputDir = "${layout.buildDirectory.get()}/generated/"
    packageName = "no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag"
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
