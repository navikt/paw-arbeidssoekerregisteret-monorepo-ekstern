plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.expediagroup.graphql")
}

val jvmMajorVersion: String by project

dependencies {
    implementation(project(":domain:error"))
    implementation(project(":lib:hoplite-config"))
    implementation(project(":lib:http-client-utils"))

    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.client.logging)
    implementation(libs.jackson.datatype.jsr310)
    api(libs.graphql.kotlin.ktor.client) {
        exclude("com.expediagroup", "graphql-kotlin-client-serialization")
    }
    api(libs.graphql.kotlin.serialization.jackson)

    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.core)
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}

graphql {
    client {
        packageName = "no.nav.paw.pdl.graphql.generated"
        schemaFile = File("src/main/resources/pdl-schema.graphql")
        queryFiles = file("src/main/resources").listFiles()?.toList()?.filter { it.name.endsWith(".graphql") }.orEmpty()
        serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.JACKSON
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
