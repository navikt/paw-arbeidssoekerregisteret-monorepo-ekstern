plugins {
    kotlin("jvm")
}

dependencies {
    implementation(jackson.kotlin)
    implementation(jackson.datatypeJsr310)
    compileOnly(apacheAvro.kafkaStreamsAvroSerde)

    testImplementation(testLibs.runnerJunit5)
    testImplementation(testLibs.assertionsCore)
    testImplementation(apacheAvro.kafkaStreamsAvroSerde)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
