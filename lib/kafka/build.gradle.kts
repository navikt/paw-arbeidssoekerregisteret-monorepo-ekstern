plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    compileOnly(libs.ktor.server.core)
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.confluent.kafka.avro.serializer)
    implementation(libs.kafka.clients)
    implementation(project(":lib:logging"))

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
