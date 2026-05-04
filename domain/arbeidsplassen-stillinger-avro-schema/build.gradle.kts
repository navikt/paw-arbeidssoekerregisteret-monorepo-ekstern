plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro")
}

val jvmMajorVersion: String by project

dependencies {
    api(libs.avro)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}
