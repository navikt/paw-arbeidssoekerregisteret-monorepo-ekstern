package no.nav.paw.oppslagapi.data.query

import no.nav.paw.oppslagapi.data.Row
import no.nav.paw.oppslagapi.data.periode_avsluttet_v1
import no.nav.paw.oppslagapi.data.periode_startet_v1
import no.nav.paw.oppslagapi.model.v2.Bekreftelse
import no.nav.paw.oppslagapi.model.v2.BekreftelseMedMetadata
import no.nav.paw.oppslagapi.model.v2.BekreftelseStatus
import no.nav.paw.oppslagapi.model.v2.Bekreftelsesloesning
import no.nav.paw.oppslagapi.model.v2.Metadata
import no.nav.paw.oppslagapi.model.v2.PaaVegneAvStart
import no.nav.paw.oppslagapi.model.v2.PaaVegneAvStopp
import java.time.Instant
import java.util.*

fun genererTidslinje(periodeId: UUID, rader: List<Row<out Any>>): GenerertTidslinje? {
    val (start, meldinger) = rader
        .filter { it.periodeId == periodeId }
        .sortedBy { it.timestamp }
        .removeFirst { melding -> melding.type == periode_startet_v1 }
    if (start == null) return null
    return meldinger.fold(
        initial = tidslinje(
            identitetsnummer = start.identitetsnummer!!,
            periodeStart = start.timestamp
        )
    ) { tidslinje, row -> tidslinje + row }
}

fun <A> List<A>.removeFirst(predicate: (A) -> Boolean): Pair<A?, List<A>> {
    val index = indexOfFirst(predicate)
    return if (index == -1) {
        null to this
    } else {
        val element = this[index]
        val newList = toMutableList().also { it.removeAt(index) }.toList()
        element to newList
    }
}

data class GenerertTidslinje(
    val identitetsnummer: String,
    val periodeStart: Instant,
    val periodeStopp: Instant?,
    val tidspunkt: Instant,
    val ansvarStart: List<PaaVegneAvStart>,
    val ansvarStopp: List<PaaVegneAvStopp>,
    val ansvarlig: Set<Bekreftelsesloesning>,
    val bekreftelser: List<BekreftelseMedMetadata>,
) {
    operator fun plus(row: Row<out Any>): GenerertTidslinje =
        when (val data = row.data) {
            is PaaVegneAvStart -> this.copy(
                tidspunkt = row.timestamp,
                ansvarStart = this.ansvarStart + data,
                ansvarlig = (this.ansvarlig + data.bekreftelsesloesning)
                    .let { it.filterNot { loesning -> loesning == Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET } }
                    .toSet()
            )

            is PaaVegneAvStopp -> this.copy(
                tidspunkt = row.timestamp,
                ansvarStopp = this.ansvarStopp + data,
                ansvarlig = (this.ansvarlig - data.bekreftelsesloesning).let {
                    it.ifEmpty {
                        setOf(Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET)
                    }
                }
            )

            is Bekreftelse -> this.copy(
                tidspunkt = row.timestamp,
                bekreftelser = this.bekreftelser + BekreftelseMedMetadata(
                    bekreftelse = data,
                    status = when {
                        periodeStopp != null -> BekreftelseStatus.UTENFOR_PERIODE
                        periodeStart > row.timestamp -> BekreftelseStatus.UTENFOR_PERIODE
                        data.bekreftelsesloesning !in this.ansvarlig -> BekreftelseStatus.UVENTET_KILDE
                        else -> BekreftelseStatus.GYLDIG
                    }
                )
            )

            is Metadata -> {
                when (row.type) {
                    periode_startet_v1 -> this
                    periode_avsluttet_v1 -> this.copy(
                        periodeStopp = row.timestamp
                    )

                    else -> throw IllegalArgumentException("Ukjent metadata kilde, type: ${row.type}")
                }
            }

            else -> this
        }
}

fun tidslinje(
    identitetsnummer: String,
    periodeStart: Instant,
): GenerertTidslinje = GenerertTidslinje(
    identitetsnummer = identitetsnummer,
    periodeStart = periodeStart,
    periodeStopp = null,
    tidspunkt = Instant.now(),
    ansvarStart = emptyList(),
    ansvarStopp = emptyList(),
    ansvarlig = setOf(Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET),
    bekreftelser = emptyList(),
)


