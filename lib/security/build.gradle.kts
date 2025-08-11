plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":lib:error-handling"))
    implementation(project(":lib:common-model"))
    implementation(project(":lib:logging"))
    implementation(libs.ktor.server.auth)
    implementation(libs.logback.classic)
    implementation(libs.nav.security.token.validation.ktor)
    implementation(libs.ktor.serialization.jackson)

    //Test
    testImplementation(project(":lib:http-client-utils"))
    testImplementation(project(":lib:pdl-client"))
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.nav.security.mock.oauth2.server)
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.jackson.datatype.jsr310)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
