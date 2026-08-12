plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String = project.property("jvmMajorVersion").toString()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

dependencies {
    api(libs.kafka.clients)
    api(project(":lib:hoplite-config"))
    api(project(":lib:kafka"))
    compileOnly(libs.kafka.streams)
    implementation(libs.logback.classic)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.annotations)
    implementation(project(":lib:logging"))

    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.kafka.streams)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
