package no.nav.paw.arbeidssoekerregisteret.api.oppslag.config

data class SchemaRegistryConfig(
    val url: String,
    val bruker: String?,
    val passord: String?,
    val autoRegistrerSchema: Boolean = true
)
