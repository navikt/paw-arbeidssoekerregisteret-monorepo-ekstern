plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.ktor.server.core)
    compileOnly(libs.kotlinx.coroutines.core)

    // Test
    testImplementation(libs.bundles.unit.testing.kotest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
