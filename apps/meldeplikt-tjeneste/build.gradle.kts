plugins {
    kotlin("jvm")
    id("com.google.cloud.tools.jib")
}

dependencies {
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:kafka-streams"))
    implementation(project(":domain:main-avro-schema"))
    implementation(orgApacheKafka.kafkaStreams)
    implementation(jackson.datatypeJsr310)
    implementation(jackson.kotlin)
    implementation(apacheAvro.kafkaStreamsAvroSerde)
}