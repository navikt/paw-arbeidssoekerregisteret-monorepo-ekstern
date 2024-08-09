package no.nav.paw.arbeidssokerregisteret.profilering.personinfo.pdl

const val PDL_CONFIG_FILE = "pdl_client_config.toml"
data class PdlConfig(
    val url: String,
    val tema: String,
    val scope: String
)