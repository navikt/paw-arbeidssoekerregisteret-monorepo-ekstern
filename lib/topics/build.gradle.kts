plugins {
    kotlin("jvm")
}

val jvmMajorVersion: String by project

dependencies {
    implementation(project(":lib:hoplite-config"))
    api(project(":lib:hoplite-config"))
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmMajorVersion))
    }
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
