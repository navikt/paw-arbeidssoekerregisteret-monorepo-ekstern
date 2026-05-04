import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask

plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro")
}

val jvmMajorVersion: String by project

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    schema(libs.nav.paw.schema.bekreftelsepaavegneav)
    implementation(libs.nav.paw.schema.bekreftelsepaavegneav)
    api(libs.avro)
}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class.java) {
    source(zipTree(schema.singleFile))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}
