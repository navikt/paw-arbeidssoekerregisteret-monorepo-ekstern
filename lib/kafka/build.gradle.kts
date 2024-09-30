plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.kotlinx.coroutines.core)
    implementation(libs.kafka.clients)
    compileOnly(libs.confluent.kafka.avro.serializer)

    // Test
    testImplementation(libs.bundles.unit.testing.kotest)
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
