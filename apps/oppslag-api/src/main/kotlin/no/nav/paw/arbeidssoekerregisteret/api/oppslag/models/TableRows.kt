package no.nav.paw.arbeidssoekerregisteret.api.oppslag.models

import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import java.time.Instant
import java.util.*

data class BrukerRow(
    val id: Long,
    val type: BrukerType,
    val brukerId: String
)

data class TidspunktFraKildeRow(
    val id: Long,
    val tidspunkt: Instant,
    val avviksType: AvviksType
)

data class MetadataRow(
    val id: Long,
    val tidspunkt: Instant,
    val utfoertAv: BrukerRow,
    val kilde: String,
    val aarsak: String,
    val tidspunktFraKilde: TidspunktFraKildeRow?
)

data class OpplysningerRow(
    val id: Long,
    val opplysningerId: UUID,
    val periodeId: UUID?
)

data class PeriodeRow(
    val id: Long,
    val periodeId: UUID,
    val identitetsnummer: String,
    val startet: MetadataRow,
    val avsluttet: MetadataRow?
)

data class PeriodeOpplysningerRow(
    val periodeId: UUID,
    val opplysningerOmArbeidssoekerTableId: Long
)

data class BekreftelseSvarRow(
    val id: Long,
    val sendtInn: MetadataRow,
    val gjelderFra: Instant,
    val gjelderTil: Instant,
    val harJobbetIDennePerioden: Boolean,
    val vilFortsetteSomArbeidssoeker: Boolean,
)

data class BekreftelseRow(
    val id: Long,
    val bekreftelseMeldingId: UUID,
    val periodeId: UUID,
    val namespace: String,
    val svar: BekreftelseSvarRow
)

infix fun BrukerRow.eq(bruker: Bruker): Boolean {
    return this.type == bruker.type && this.brukerId == bruker.id
}
