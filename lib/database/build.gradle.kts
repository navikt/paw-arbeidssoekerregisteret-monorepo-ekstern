plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    compileOnly(libs.ktor.server.core)
    compileOnly(libs.hikari.connection.pool)
    compileOnly(libs.exposed.jdbc)
    compileOnly(libs.flyway.postgres)

    // Test
    testImplementation(libs.bundles.unit.testing.kotest)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
