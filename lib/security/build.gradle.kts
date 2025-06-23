plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":lib:error-handling"))
    api(project(":lib:common-model"))
    implementation(libs.ktor.server.auth)
    implementation(libs.logback.classic)
    implementation(libs.nav.security.token.validation.ktor)

    //Test
    testImplementation(libs.nav.security.mock.oauth2.server)
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.jackson)
    testImplementation(libs.jackson.datatype.jsr310)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
