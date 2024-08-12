import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask

plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro")
    id("com.google.cloud.tools.jib")
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
    implementation(project(":domain:main-avro-schema"))
    implementation(pawClients.pawAaregClient)
    implementation(pawClients.pawPdlClient)
    implementation(project(":lib:app-config"))

    implementation(navCommon.tokenClient)

    //Ktor
    implementation(ktor.serializationJackson)
    //ktor server
    implementation(ktorServer.core)
    implementation(ktorServer.coreJvm)
    implementation(ktorServer.netty)
    implementation(ktorServer.micrometer)
    implementation(ktorServer.statusPages)
    // Ktor client
    implementation(ktorClient.okhttp)
    implementation(ktorClient.loggingJvm)
    implementation(ktorClient.contentNegotiation)

    // Micrometer
    implementation(micrometer.registryPrometheus)

    // Logging
    implementation("no.nav.common:log:$navCommonModulesVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashVersion")

    // Kafka
    implementation(project(":lib:kafka"))
    implementation(project(":lib:kafka-streams"))
    implementation(apacheAvro.kafkaSerializer)
    implementation(apacheAvro.kafkaStreamsAvroSerde)
    implementation(apacheAvro.avro)
    implementation(orgApacheKafka.kafkaClients)
    implementation(orgApacheKafka.kafkaStreams)

    // Jackson
    implementation(jackson.datatypeJsr310)
    implementation(jackson.kotlin)

    // kotest
    testImplementation(testLibs.runnerJunit5)
    testImplementation(orgApacheKafka.streamsTest)

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
