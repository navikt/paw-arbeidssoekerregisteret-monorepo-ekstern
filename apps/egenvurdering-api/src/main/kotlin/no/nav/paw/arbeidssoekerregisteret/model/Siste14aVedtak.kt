package no.nav.paw.arbeidssoekerregisteret.model

import no.nav.common.types.identer.AktorId
import java.time.Instant

data class Siste14aVedtak(
    val aktorId: AktorId,
    val innsatsgruppe: String?, // Dette er enumererte verdier fra Innsaktsgruppe enumen
    val hovedmal: String?, // Dette er enumererte verdier fra Hovedmal enumen
    val fattetDato: Instant,
    val fraArena: Boolean
)

data class Beriket14aVedtak(
    val aktorId: String,
    val arbeidssoekerId: Long,
    val innsatsgruppe: String?, // Dette er enumererte verdier fra Innsaktsgruppe enumen
    val hovedmal: String?, // Dette er enumererte verdier fra Hovedmal enumen
    val fattetDato: Instant,
    val fraArena: Boolean
)

fun Siste14aVedtak.tilBeriket14aVedtak(
    arbeidssoekerId: Long
): Beriket14aVedtak {
    return Beriket14aVedtak(
        aktorId.get(),
        arbeidssoekerId,
        innsatsgruppe,
        hovedmal,
        fattetDato,
        fraArena
    )
}

data class Siste14aVedtakInfo(
    val aktorId: String,
    val arbeidssoekerId: Long,
    var fattetDato: Instant
)

