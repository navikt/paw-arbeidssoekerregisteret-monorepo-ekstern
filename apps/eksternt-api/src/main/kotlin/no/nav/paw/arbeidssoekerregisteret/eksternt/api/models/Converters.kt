package no.nav.paw.arbeidssoekerregisteret.eksternt.api.models

import io.ktor.server.plugins.BadRequestException
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.toLocalDateTime
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.model.asIdentitetsnummer
import org.jetbrains.exposed.v1.core.ResultRow

import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

fun EksternRequest.getIdentitetsnummer(): Identitetsnummer = this.identitetsnummer.asIdentitetsnummer()
fun EksternRequest.getFraStartetDato(): LocalDate? {
    return this.fraStartetDato?.let {
        try {
            LocalDate.parse(it)
        } catch (e: DateTimeParseException) {
            throw BadRequestException("Ugyldig dato \"fraStartetDato\" må ha format \"yyyy-mm-dd\"")
        }
    }
}

fun PeriodeRow.asArbeidssoekerperiodeResponse() =
    ArbeidssoekerperiodeResponse(
        periodeId = periodeId,
        startet = startet.toLocalDateTime().truncatedTo(ChronoUnit.MILLIS),
        avsluttet = avsluttet?.toLocalDateTime()?.truncatedTo(ChronoUnit.MILLIS),
    )

fun Periode.asPeriodeRow() =
    PeriodeRow(
        id = -1, // Ikke viktig. Blir generert av databasen ved insert.
        identitetsnummer = identitetsnummer,
        periodeId = id,
        startet = startet.tidspunkt,
        avsluttet = avsluttet?.tidspunkt
    )

fun ResultRow.asPeriodeRow(): PeriodeRow =
    PeriodeRow(
        id = get(PeriodeTable.id).value,
        identitetsnummer = get(PeriodeTable.identitetsnummer),
        periodeId = get(PeriodeTable.periodeId),
        startet = get(PeriodeTable.startet),
        avsluttet = get(PeriodeTable.avsluttet)
    )
