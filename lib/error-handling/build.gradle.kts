plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    compileOnly(libs.ktor.server.cors)
    compileOnly(libs.ktor.server.status.pages)
    compileOnly(libs.ktor.serialization.jackson)
    compileOnly(libs.kafka.streams)
    compileOnly(libs.logback.classic)

    //Test
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.ktor.server.content.negotiation)
    testImplementation(libs.ktor.server.status.pages)
    testImplementation(libs.ktor.serialization.jackson)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.kafka.streams)
    testImplementation(libs.logback.classic)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
