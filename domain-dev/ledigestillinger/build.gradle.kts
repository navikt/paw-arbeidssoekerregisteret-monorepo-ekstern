plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    api(libs.jackson.kotlin)
    api(libs.jackson.datatype.jsr310)
    testImplementation(libs.bundles.unit.testing.kotest)
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
