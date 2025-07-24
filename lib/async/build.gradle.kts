plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    compileOnly(libs.kotlinx.coroutines.core)
    compileOnly(libs.logback.classic)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}