plugins {
    kotlin("jvm")
}

dependencies {

    implementation(libs.ktor.server.core)
    compileOnly(libs.kafka.streams)

    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.kafka.streams)
    testImplementation(libs.ktor.server.test.host)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
