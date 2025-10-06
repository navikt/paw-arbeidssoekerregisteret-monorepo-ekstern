plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project
val jvmVersion = JavaVersion.valueOf("VERSION_$jvmMajorVersion")

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:http-client-utils"))
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.kotlin)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.cio)
    implementation(libs.nav.security.token.client)
    api(libs.nav.common.token.client)

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.bundles.unit.testing.kotest)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(jvmVersion.majorVersion)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
