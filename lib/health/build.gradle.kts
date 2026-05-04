plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {

    implementation(libs.ktor.server.core)
    compileOnly(libs.kafka.streams)

    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.kafka.streams)
    testImplementation(libs.ktor.server.test.host)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
