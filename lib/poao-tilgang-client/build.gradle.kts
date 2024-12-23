plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

val jvmMajorVersion: String by project

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:http-client-utils"))

    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.client.logging)
    implementation(libs.jackson.datatype.jsr310)
    api(libs.nav.poao.tilgang.api)
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.ktor.client.mock)
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
