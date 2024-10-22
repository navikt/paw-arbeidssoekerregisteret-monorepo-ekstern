package no.nav.paw.arbeidssoekerregisteret.api.oppslag.models

import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.bekreftelse.melding.v1.vo.Bekreftelsesloesning
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
    val periodeId: UUID,
    val sendtInnAv: MetadataRow,
    val jobbsituasjon: List<BeskrivelseMedDetaljerRow>,
    val utdanning: UtdanningRow?,
    val helse: HelseRow?,
    val annet: AnnetRow?
)

data class BeskrivelseMedDetaljerRow(
    val id: Long,
    val beskrivelse: Beskrivelse,
    val detaljer: List<DetaljerRow>
)

data class DetaljerRow(
    val id: Long,
    val noekkel: String,
    val verdi: String
)

data class UtdanningRow(
    val id: Long,
    val nus: String,
    val bestaatt: JaNeiVetIkke?,
    val godkjent: JaNeiVetIkke?
)

data class HelseRow(
    val id: Long,
    val helsetilstandHindrerArbeid: JaNeiVetIkke
)

data class AnnetRow(
    val id: Long,
    val andreForholdHindrerArbeid: JaNeiVetIkke?
)

data class OpplysningerMarkerRow(
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

data class ProfileringRow(
    val id: Long,
    val profileringId: UUID,
    val periodeId: UUID,
    val opplysningerOmArbeidssoekerId: UUID,
    val sendtInnAv: MetadataRow,
    val profilertTil: ProfilertTil,
    val jobbetSammenhengendeSeksAvTolvSisteManeder: Boolean? = null,
    val alder: Int? = null
)

data class BekreftelseRow(
    val id: Long,
    val bekreftelseId: UUID,
    val periodeId: UUID,
    val bekreftelsesloesning: Bekreftelsesloesning,
    val svar: BekreftelseSvarRow
)

data class BekreftelseSvarRow(
    val id: Long,
    val sendtInn: MetadataRow,
    val gjelderFra: Instant,
    val gjelderTil: Instant,
    val harJobbetIDennePerioden: Boolean,
    val vilFortsetteSomArbeidssoeker: Boolean,
)
