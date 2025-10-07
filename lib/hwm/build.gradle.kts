plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    implementation(libs.exposed.jdbc.v1)
    implementation(libs.opentelemetry.annotations)
    compileOnly(libs.micrometer.registry.prometheus)
    compileOnly(libs.kafka.clients)
    implementation(project(":lib:health"))

    // Test
    testImplementation(libs.hikari.connection.pool)
    testImplementation(libs.postgres.driver)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.nav.security.mock.oauth2.server)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(project(":lib:database"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
