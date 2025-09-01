plugins {
    id("com.google.cloud.tools.jib")
}

val distrolessJavaImage: String by project
val image: String? by project
val targetImage: String = "${image ?: project.name}:${project.version}"

jib {
    from.image = distrolessJavaImage
    to.image = targetImage
    container {
        jvmFlags = listOf("-XX:ActiveProcessorCount=8", "-XX:+UseZGC", "-XX:+ZGenerational")
        environment = mapOf(
            "IMAGE_WITH_VERSION" to targetImage
        )
    }
}
