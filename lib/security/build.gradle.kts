plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":lib:error-handling"))
    implementation(libs.ktor.server.auth)
    implementation(libs.logback.classic)
    implementation(libs.nav.security.token.validation.ktor)

    //Test
    testImplementation(project(":lib:pdl-client"))
    testImplementation(libs.nav.poao.tilgang.client)
    testImplementation(libs.nav.security.mock.oauth2.server)
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.jackson)
    testImplementation(libs.jackson.datatype.jsr310)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
