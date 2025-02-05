plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.ktor.server.core)
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.confluent.kafka.avro.serializer)
    implementation(libs.kafka.clients)

    // Test
    testImplementation(libs.bundles.unit.testing.kotest)
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
