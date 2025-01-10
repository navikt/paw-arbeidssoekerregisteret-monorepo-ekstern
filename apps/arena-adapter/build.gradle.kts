plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.google.jib)
    application
}

val jvmMajorVersion: String by project
val baseImage: String by project
val image: String? by project

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:arena-avro-schema"))

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

jib {
    from.image = "$baseImage:$jvmMajorVersion"
    to.image = "${image ?: project.name}:${project.version}"
    container {
        environment = mapOf(
            "IMAGE_WITH_VERSION" to "${image ?: project.name}:${project.version}"
        )
        jvmFlags = listOf(
            "-XX:ActiveProcessorCount=4", "-XX:+UseZGC", "-XX:+ZGenerational"
        )
    }
}
