package no.nav.paw.kafkakeygenerator.client

data class IdentitetRequest(
    val identitet: String
)

data class IdentiteterResponse(
    val arbeidssoekerId: Long? = null,
    val recordKey: Long? = null,
    val identiteter: List<Identitet>,
    val pdlIdentiteter: List<Identitet>? = null,
    val konflikter: List<Konflikt> = emptyList()
)

data class Identitet(
    val identitet: String,
    val type: IdentitetType,
    val gjeldende: Boolean
)

enum class IdentitetType {
    FOLKEREGISTERIDENT, AKTORID, NPID, ARBEIDSSOEKERID, UKJENT_VERDI
}

data class Konflikt(
    val type: KonfliktType,
    val detaljer: MergeKonflikt? = null
)

enum class KonfliktType { MERGE, SPLITT, SLETT }

data class MergeKonflikt(
    val aktorIdListe: List<String>,
    val arbeidssoekerIdListe: List<Long>
)

data class ProblemDetails(
    val id: String,
    val type: String,
    val status: Int,
    val title: String,
    val detail: String,
    val instance: String,
    val timestamp: String
)