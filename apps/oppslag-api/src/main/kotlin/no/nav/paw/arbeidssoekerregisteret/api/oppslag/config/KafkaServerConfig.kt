package no.nav.paw.arbeidssoekerregisteret.api.oppslag.config

data class KafkaServerConfig(
    val autentisering: String,
    val kafkaBrokers: String,
    val keystorePath: String?,
    val credstorePassword: String?,
    val truststorePath: String?
)
