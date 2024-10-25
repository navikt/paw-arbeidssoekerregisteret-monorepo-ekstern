plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.graphql)
}

val jvmMajorVersion: String by project

dependencies {
    api(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    api(libs.graphql.ktor.client)

    testImplementation(libs.bundles.unit.testing.kotest)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.core)
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}


graphql {
    client {
        packageName = "no.nav.paw.pdl.graphql.generated"
        schemaFile = File("src/main/resources/pdl-schema.graphql")
        queryFiles = file("src/main/resources").listFiles()?.toList()?.filter { it.name.endsWith(".graphql") }.orEmpty()
        serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.KOTLINX
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
