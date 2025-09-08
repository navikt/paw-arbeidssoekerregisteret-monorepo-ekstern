plugins {
    id("com.google.cloud.tools.jib")
}

val chainguardJavaImage = "europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21"
val image: String? by project
val targetImage: String = "${image ?: project.name}:${project.version}"

jib {
    from.image = chainguardJavaImage
    to.image = targetImage
    container {
        jvmFlags = listOf("-XX:ActiveProcessorCount=8", "-XX:+UseZGC", "-XX:+ZGenerational")
        environment = mapOf(
            "IMAGE_WITH_VERSION" to targetImage
        )
    }
}
