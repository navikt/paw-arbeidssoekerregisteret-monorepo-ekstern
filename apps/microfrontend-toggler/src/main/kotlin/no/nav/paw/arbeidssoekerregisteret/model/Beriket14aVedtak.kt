package no.nav.paw.arbeidssoekerregisteret.model

import java.time.Instant

data class Beriket14aVedtak(
    val aktorId: String,
    val arbeidssoekerId: Long,
    val innsatsgruppe: String?, // Dette er enumererte verdier fra Innsaktsgruppe enumen
    val hovedmal: String?, // Dette er enumererte verdier fra Hovedmal enumen
    val fattetDato: Instant,
    val fraArena: Boolean
)