plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    implementation(libs.hoplite.toml)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
