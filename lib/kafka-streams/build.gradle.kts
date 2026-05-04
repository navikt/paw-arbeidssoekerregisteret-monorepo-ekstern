plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    api(project(":lib:kafka"))
    compileOnly(libs.ktor.server.core)
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams)
    implementation(libs.confluent.kafka.streams.avro.serde)

    // Test
    testImplementation(libs.bundles.unit.testing.kotest)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
