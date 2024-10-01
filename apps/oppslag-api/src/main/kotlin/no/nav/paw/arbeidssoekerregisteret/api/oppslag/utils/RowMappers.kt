package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.AnnetTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BeskrivelseMedDetaljerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BeskrivelseTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BrukerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.HelseTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.MetadataTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.OpplysningerOmArbeidssoekerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeOpplysningerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.ProfileringTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.TidspunktFraKildeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.UtdanningTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.AnnetResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BeskrivelseMedDetaljerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.HelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.JaNeiVetIkke
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.JobbSituasjonBeskrivelse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.MetadataRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeOpplysningerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.TidspunktFraKildeRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.UtdanningResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toJaNeiVetIkke
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toJobbSituasjonBeskrivelse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toMetadataResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toProfileringsResultat
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toOpplysningerResponse(beskrivelseMedDetaljer: List<BeskrivelseMedDetaljerResponse>): OpplysningerOmArbeidssoekerResponse {
    val opplysningerId = get(OpplysningerOmArbeidssoekerTable.opplysningerOmArbeidssoekerId)
    val periodeId = get(PeriodeOpplysningerTable.periodeId)
    val utdanningId = getOrNull(OpplysningerOmArbeidssoekerTable.utdanningId)
    val helseId = getOrNull(OpplysningerOmArbeidssoekerTable.helseId)
    val annetId = getOrNull(OpplysningerOmArbeidssoekerTable.annetId)

    val sendtInnAvMetadata = toMetadataRow().toMetadataResponse()
    val utdanning = utdanningId?.let { toUtdanningResponse() }
    val helse = helseId?.let { toHelseResponse() }
    val annet = annetId?.let { toAnnetResponse() }

    return OpplysningerOmArbeidssoekerResponse(
        opplysningerId,
        periodeId,
        sendtInnAvMetadata,
        beskrivelseMedDetaljer,
        utdanning,
        helse,
        annet
    )
}

private fun ResultRow.toUtdanningResponse(): UtdanningResponse {
    val nus = get(UtdanningTable.nus)
    val bestaatt = getOrNull(UtdanningTable.bestaatt)?.toJaNeiVetIkke()
    val godkjent = getOrNull(UtdanningTable.godkjent)?.toJaNeiVetIkke()
    return UtdanningResponse(nus, bestaatt, godkjent)
}

private fun ResultRow.toHelseResponse() =
    HelseResponse(
        get(HelseTable.helsetilstandHindrerArbeid).toJaNeiVetIkke()
    )

private fun ResultRow.toAnnetResponse() =
    AnnetResponse(
        getOrNull(AnnetTable.andreForholdHindrerArbeid)?.toJaNeiVetIkke()
    )

fun ResultRow.toBeskrivelseMedDetaljerResponse(detaljer: Map<String, String>): BeskrivelseMedDetaljerResponse {
    val id = get(BeskrivelseMedDetaljerTable.id)
    val beskrivelse = toBeskrivelseResponse() ?: throw RuntimeException("Fant ikke beskrivelse: $id")
    return BeskrivelseMedDetaljerResponse(beskrivelse, detaljer)
}

private fun ResultRow.toBeskrivelseResponse(): JobbSituasjonBeskrivelse? {
    return getOrNull(BeskrivelseTable.beskrivelse)?.toJobbSituasjonBeskrivelse()
}

fun ResultRow.toProfileringResponse(): ProfileringResponse {
    val profileringId = get(ProfileringTable.profileringId)
    val periodeId = get(ProfileringTable.periodeId)
    val opplysningerOmArbeidssoekerId = get(ProfileringTable.opplysningerOmArbeidssoekerId)
    val profilertTil = get(ProfileringTable.profilertTil)
    val jobbetSammenhengendeSeksAvTolvSisteManeder = get(ProfileringTable.jobbetSammenhengendeSeksAvTolvSisteManeder)
    val alder = get(ProfileringTable.alder)
    val sendtInnAv = toMetadataRow().toMetadataResponse()
    return ProfileringResponse(
        profileringId,
        periodeId,
        opplysningerOmArbeidssoekerId,
        sendtInnAv,
        profilertTil.toProfileringsResultat(),
        jobbetSammenhengendeSeksAvTolvSisteManeder,
        alder
    )
}

private fun ResultRow.toBrukerRow(): BrukerRow {
    val id = get(BrukerTable.id).value
    val brukerId = get(BrukerTable.brukerId)
    val type = get(BrukerTable.type)
    return BrukerRow(id, type, brukerId)
}

private fun ResultRow.toTidspunktFraKildeRow(): TidspunktFraKildeRow {
    val id = get(TidspunktFraKildeTable.id).value
    val tidspunkt = get(TidspunktFraKildeTable.tidspunkt)
    val avviksType = get(TidspunktFraKildeTable.avviksType)
    return TidspunktFraKildeRow(id, tidspunkt, avviksType)
}

fun ResultRow.toMetadataRow(): MetadataRow {
    val id = get(MetadataTable.id).value
    val tidspunkt = get(MetadataTable.tidspunkt)
    val bruker = toBrukerRow()
    val kilde = get(MetadataTable.kilde)
    val aarsak = get(MetadataTable.aarsak)
    val tidspunktFraKildeId = getOrNull(MetadataTable.tidspunktFraKildeId)
    val tidspunktFraKilde = tidspunktFraKildeId?.let { toTidspunktFraKildeRow() }
    return MetadataRow(id, tidspunkt, bruker, kilde, aarsak, tidspunktFraKilde)
}

fun ResultRow.toOpplysningerRow(): OpplysningerRow {
    val id = get(OpplysningerOmArbeidssoekerTable.id).value
    val opplysningerId = get(OpplysningerOmArbeidssoekerTable.opplysningerOmArbeidssoekerId)
    val periodeId = get(PeriodeOpplysningerTable.periodeId)
    return OpplysningerRow(id, opplysningerId, periodeId)
}

fun ResultRow.toPeriodeRow(): PeriodeRow {
    val id = get(PeriodeTable.id).value
    val periodeId = get(PeriodeTable.periodeId)
    val identitetsnummer = get(PeriodeTable.identitetsnummer)
    val avsluttetMetadataId = getOrNull(PeriodeTable.avsluttetId)
    val startetMetadata = toStartetMetadataRow()
    val avsluttetMetadata = avsluttetMetadataId?.let { toAvsluttetMetadataRow() }
    return PeriodeRow(id, periodeId, identitetsnummer, startetMetadata, avsluttetMetadata)
}

fun ResultRow.toPeriodeOpplysningerRow(): PeriodeOpplysningerRow {
    val periodeId = get(PeriodeOpplysningerTable.periodeId)
    val opplysningerOmArbeidssoekerTableId = get(PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId)
    return PeriodeOpplysningerRow(periodeId, opplysningerOmArbeidssoekerTableId)
}

private fun ResultRow.toStartetMetadataRow(): MetadataRow {
    val id = get(StartetMetadataAlias[MetadataTable.id]).value
    val tidspunkt = get(StartetMetadataAlias[MetadataTable.tidspunkt])
    val bruker = toStartetBrukerRow()
    val kilde = get(StartetMetadataAlias[MetadataTable.kilde])
    val aarsak = get(StartetMetadataAlias[MetadataTable.aarsak])
    val tidspunktFraKildeId = getOrNull(StartetMetadataAlias[MetadataTable.tidspunktFraKildeId])
    val tidspunktFraKilde = tidspunktFraKildeId?.let { toStartetTidspunktFraKildeRow() }
    return MetadataRow(id, tidspunkt, bruker, kilde, aarsak, tidspunktFraKilde)
}

private fun ResultRow.toAvsluttetMetadataRow(): MetadataRow {
    val id = get(AvsluttetMetadataAlias[MetadataTable.id]).value
    val tidspunkt = get(AvsluttetMetadataAlias[MetadataTable.tidspunkt])
    val bruker = toAvsluttetBrukerRow()
    val kilde = get(AvsluttetMetadataAlias[MetadataTable.kilde])
    val aarsak = get(AvsluttetMetadataAlias[MetadataTable.aarsak])
    val tidspunktFraKildeId = getOrNull(AvsluttetMetadataAlias[MetadataTable.tidspunktFraKildeId])
    val tidspunktFraKilde = tidspunktFraKildeId?.let { toAvsluttetTidspunktFraKildeRow() }
    return MetadataRow(id, tidspunkt, bruker, kilde, aarsak, tidspunktFraKilde)
}

private fun ResultRow.toStartetBrukerRow(): BrukerRow {
    val id = get(StartetBrukerAlias[BrukerTable.id]).value
    val type = get(StartetBrukerAlias[BrukerTable.type])
    val brukerId = get(StartetBrukerAlias[BrukerTable.brukerId])
    return BrukerRow(id, type, brukerId)
}

private fun ResultRow.toAvsluttetBrukerRow(): BrukerRow {
    val id = get(AvsluttetBrukerAlias[BrukerTable.id]).value
    val type = get(AvsluttetBrukerAlias[BrukerTable.type])
    val brukerId = get(AvsluttetBrukerAlias[BrukerTable.brukerId])
    return BrukerRow(id, type, brukerId)
}

private fun ResultRow.toStartetTidspunktFraKildeRow(): TidspunktFraKildeRow {
    val id = get(StartetTidspunktAlias[TidspunktFraKildeTable.id]).value
    val tidspunkt = get(StartetTidspunktAlias[TidspunktFraKildeTable.tidspunkt])
    val avviksType = get(StartetTidspunktAlias[TidspunktFraKildeTable.avviksType])
    return TidspunktFraKildeRow(id, tidspunkt, avviksType)
}

private fun ResultRow.toAvsluttetTidspunktFraKildeRow(): TidspunktFraKildeRow {
    val id = get(AvsluttetTidspunktAlias[TidspunktFraKildeTable.id]).value
    val tidspunkt = get(AvsluttetTidspunktAlias[TidspunktFraKildeTable.tidspunkt])
    val avviksType = get(AvsluttetTidspunktAlias[TidspunktFraKildeTable.avviksType])
    return TidspunktFraKildeRow(id, tidspunkt, avviksType)
}
