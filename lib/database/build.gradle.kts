plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.ktor.server.core)
    compileOnly(libs.hikari.connection.pool)
    compileOnly(libs.exposed.core)
    compileOnly(libs.flyway.postgres)

    // Test
    testImplementation(libs.bundles.unit.testing.kotest)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
