package no.nav.paw.arbeidssoekerregisteret.model

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode

fun Periode.asPeriodeInfo(arbeidssoekerId: Long): PeriodeInfo {
    return PeriodeInfo(
        id = id,
        identitetsnummer = identitetsnummer,
        arbeidssoekerId = arbeidssoekerId,
        startet = startet.tidspunkt,
        avsluttet = avsluttet?.tidspunkt
    )
}