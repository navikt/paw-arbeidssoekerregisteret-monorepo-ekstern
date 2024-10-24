package no.nav.paw.arbeidssoekerregisteret.eksternt.api.models

import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.toLocalDateTime
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode

fun Arbeidssoekerperiode.toArbeidssoekerperiodeResponse() =
    ArbeidssoekerperiodeResponse(
        periodeId = periodeId,
        startet = startet,
        avsluttet = avsluttet
    )

fun Periode.toArbeidssoekerperiode() =
    Arbeidssoekerperiode(
        identitetsnummer = Identitetsnummer(identitetsnummer),
        periodeId = id,
        startet = startet.tidspunkt.toLocalDateTime(),
        avsluttet = avsluttet?.tidspunkt?.toLocalDateTime()
    )

fun EksternRequest.getIdentitetsnummer() = this.identitetsnummer.toIdentitetsnummer()
