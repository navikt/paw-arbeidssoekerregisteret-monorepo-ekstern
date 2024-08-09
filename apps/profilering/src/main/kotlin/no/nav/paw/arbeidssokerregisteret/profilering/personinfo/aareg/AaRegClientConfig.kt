package no.nav.paw.arbeidssokerregisteret.profilering.personinfo.aareg

const val AAREG_CONFIG_FILE = "aareg_client_config.toml"
data class AaRegClientConfig(
    val url: String,
    val scope: String
)