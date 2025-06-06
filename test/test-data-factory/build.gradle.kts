plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":domain:main-avro-schema"))
    implementation(project(":domain:bekreftelsesmelding-avro-schema"))
    implementation(project(":domain:bekreftelse-paavegneav-avro-schema"))
}
