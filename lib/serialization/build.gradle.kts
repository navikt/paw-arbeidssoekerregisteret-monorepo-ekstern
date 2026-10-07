plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    compileOnly(libs.ktor.server.core)
    compileOnly(libs.ktor.server.content.negotiation)
    compileOnly(libs.ktor.serialization.jackson)
    compileOnly(libs.jackson.kotlin)

    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(project(":domain:error"))
    testImplementation(project(":lib:http-client-utils"))
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.jackson.kotlin)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
