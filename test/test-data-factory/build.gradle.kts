plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:bekreftelsesmelding-avro-schema"))
    implementation(project(":domain:bekreftelse-paavegneav-avro-schema"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}
