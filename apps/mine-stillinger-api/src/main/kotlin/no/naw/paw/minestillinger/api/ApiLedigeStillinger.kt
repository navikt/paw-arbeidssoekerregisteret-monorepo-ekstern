package no.naw.paw.minestillinger.api

import java.time.Instant

data class ApiLedigeStillinger(
    val soek: List<ApiStillingssoek>,
    val resultat: List<JobbAnnonse>
)

data class JobbAnnonse(
    val tittel: String,
    val stillingbeskrivelse: String?,
    val publisert: Instant,
    val soeknadsfrist: Instant?,
    val land: String,
    val kommune: String?
)