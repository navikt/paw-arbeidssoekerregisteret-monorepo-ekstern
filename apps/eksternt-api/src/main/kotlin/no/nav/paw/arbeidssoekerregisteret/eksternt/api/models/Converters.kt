package no.nav.paw.arbeidssoekerregisteret.eksternt.api.models

import no.nav.paw.arbeidssoekerregisteret.eksternt.api.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.toLocalDateTime
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import org.jetbrains.exposed.sql.ResultRow

fun PeriodeRow.asArbeidssoekerperiodeResponse() =
    ArbeidssoekerperiodeResponse(
        periodeId = periodeId,
        startet = startet.toLocalDateTime(),
        avsluttet = avsluttet?.toLocalDateTime()
    )

fun Periode.asPeriodeRow() =
    PeriodeRow(
        id = -1,
        identitetsnummer = identitetsnummer,
        periodeId = id,
        startet = startet.tidspunkt,
        avsluttet = avsluttet?.tidspunkt
    )

fun EksternRequest.getIdentitetsnummer() = this.identitetsnummer.toIdentitetsnummer()

fun ResultRow.asPeriodeRow(): PeriodeRow =
    PeriodeRow(
        id = get(PeriodeTable.id).value,
        identitetsnummer = get(PeriodeTable.identitetsnummer),
        periodeId = get(PeriodeTable.periodeId),
        startet = get(PeriodeTable.startet),
        avsluttet = get(PeriodeTable.avsluttet)
    )
