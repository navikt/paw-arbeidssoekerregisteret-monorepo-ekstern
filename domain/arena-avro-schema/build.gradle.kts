import com.github.davidmc24.gradle.plugin.avro.GenerateAvroProtocolTask

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.avro)
}

val schema by configurations.creating {
    isTransitive = false
}

dependencies {
    schema(libs.nav.paw.schema.arena)
    implementation(libs.nav.paw.schema.arena)
    api(libs.avro)
}

tasks.named("generateAvroProtocol", GenerateAvroProtocolTask::class.java) {
    source(zipTree(schema.singleFile))
}
