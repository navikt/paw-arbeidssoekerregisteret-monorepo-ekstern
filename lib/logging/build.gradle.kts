plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    compileOnly(project(":lib:hoplite-config"))
    compileOnly(libs.ktor.server.core)
    compileOnly(libs.ktor.server.callid)
    compileOnly(libs.ktor.server.call.logging)
    compileOnly(libs.logback.classic)
    compileOnly(libs.logstash.logback.encoder)
    compileOnly(libs.nav.common.log)
    compileOnly(libs.nav.common.audit.log)

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