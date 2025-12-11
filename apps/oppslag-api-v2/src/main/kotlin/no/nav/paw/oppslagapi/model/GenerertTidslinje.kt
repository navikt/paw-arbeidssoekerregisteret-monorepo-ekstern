package no.nav.paw.oppslagapi.model

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