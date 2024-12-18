plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":lib:kafka"))
    compileOnly(libs.ktor.server.core)
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams)
    implementation(libs.confluent.kafka.streams.avro.serde)

    // Test
    testImplementation(libs.bundles.unit.testing.kotest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
