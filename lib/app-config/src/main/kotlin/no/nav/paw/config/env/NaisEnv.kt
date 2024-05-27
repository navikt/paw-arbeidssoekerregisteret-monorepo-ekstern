package no.nav.paw.config.env

enum class NaisEnv(val clusterName: String) {
    Local("local"),
    DevGCP("dev-gcp"),
    ProdGCP("prod-gcp")
}

val currentNaisEnv: NaisEnv
    get() =
        when (System.getenv("NAIS_CLUSTER_NAME")) {
            NaisEnv.DevGCP.clusterName -> NaisEnv.DevGCP
            NaisEnv.ProdGCP.clusterName -> NaisEnv.ProdGCP
            else -> NaisEnv.Local
        }

val currentAppId get() = System.getenv("IMAGE_WITH_VERSION") ?: "UNSPECIFIED"
