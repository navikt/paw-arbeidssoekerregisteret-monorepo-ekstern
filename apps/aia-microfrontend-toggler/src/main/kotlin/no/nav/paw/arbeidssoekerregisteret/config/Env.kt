package no.nav.paw.arbeidssoekerregisteret.config

import java.lang.System.getenv

enum class Env(val clusterName: String) {
    Local("local"),
    DevGCP("dev-gcp"),
    ProdGCP("prod-gcp")
}

val currentEnv: Env
    get() =
        when (getenv("NAIS_CLUSTER_NAME")) {
            Env.DevGCP.clusterName -> Env.DevGCP
            Env.ProdGCP.clusterName -> Env.ProdGCP
            else -> Env.Local
        }

val currentAppId get() = getenv("IMAGE_WITH_VERSION") ?: "UNSPECIFIED"
