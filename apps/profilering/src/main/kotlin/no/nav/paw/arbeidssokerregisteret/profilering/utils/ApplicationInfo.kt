package no.nav.paw.arbeidssokerregisteret.profilering.utils

object ApplicationInfo {
    val version: String? = System.getenv("PROFILERING_APPLICATION_VERSION")
    val name: String? = System.getenv("PROFILERING_APPLICATION_ID")
    val id get() = "$name-$version"
}