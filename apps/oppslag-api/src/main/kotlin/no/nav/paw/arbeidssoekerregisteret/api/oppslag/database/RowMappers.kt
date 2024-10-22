package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.AnnetRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BekreftelseRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BekreftelseSvarRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BeskrivelseMedDetaljerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.DetaljerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.HelseRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.MetadataRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerMarkerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeOpplysningerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.TidspunktFraKildeRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.UtdanningRow
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toBekreftelseRow(): BekreftelseRow {
    return BekreftelseRow(
        id = get(BekreftelseTable.id).value,
        bekreftelseId = get(BekreftelseTable.bekreftelseId),
        periodeId = get(BekreftelseTable.periodeId),
        bekreftelsesloesning = get(BekreftelseTable.bekreftelsesloesning),
        svar = toBekreftelseSvarRow()
    )
}

private fun ResultRow.toBekreftelseSvarRow(): BekreftelseSvarRow {
    return BekreftelseSvarRow(
        id = get(BekreftelseSvarTable.id).value,
        sendtInn = toMetadataRow(),
        gjelderFra = get(BekreftelseSvarTable.gjelderFra),
        gjelderTil = get(BekreftelseSvarTable.gjelderTil),
        harJobbetIDennePerioden = get(BekreftelseSvarTable.harJobbetIDennePerioden),
        vilFortsetteSomArbeidssoeker = get(BekreftelseSvarTable.vilFortsetteSomArbeidssoeker)
    )
}

fun ResultRow.toOpplysningerRow(jobbsituasjon: List<BeskrivelseMedDetaljerRow>): OpplysningerRow {
    val utdanningId = getOrNull(OpplysningerOmArbeidssoekerTable.utdanningId)
    val helseId = getOrNull(OpplysningerOmArbeidssoekerTable.helseId)
    val annetId = getOrNull(OpplysningerOmArbeidssoekerTable.annetId)
    return OpplysningerRow(
        id = get(OpplysningerOmArbeidssoekerTable.id).value,
        opplysningerId = get(OpplysningerOmArbeidssoekerTable.opplysningerOmArbeidssoekerId),
        periodeId = get(PeriodeOpplysningerTable.periodeId),
        sendtInnAv = toMetadataRow(),
        jobbsituasjon = jobbsituasjon,
        utdanning = utdanningId?.let { toUtdanningRow() },
        helse = helseId?.let { toHelseRow() },
        annet = annetId?.let { toAnnetRow() }
    )
}

fun ResultRow.toBeskrivelseMedDetaljerRow(detaljer: List<DetaljerRow>): BeskrivelseMedDetaljerRow {
    return BeskrivelseMedDetaljerRow(
        id = get(BeskrivelseMedDetaljerTable.id).value,
        beskrivelse = get(BeskrivelseTable.beskrivelse),
        detaljer = detaljer
    )
}

fun ResultRow.toDetaljerRow(): DetaljerRow {
    return DetaljerRow(
        id = get(DetaljerTable.id).value,
        noekkel = get(DetaljerTable.noekkel),
        verdi = get(DetaljerTable.verdi)
    )
}

fun ResultRow.toUtdanningRow(): UtdanningRow {
    return UtdanningRow(
        id = get(UtdanningTable.id).value,
        nus = get(UtdanningTable.nus),
        bestaatt = getOrNull(UtdanningTable.bestaatt),
        godkjent = getOrNull(UtdanningTable.godkjent)
    )
}

fun ResultRow.toHelseRow(): HelseRow {
    return HelseRow(
        id = get(HelseTable.id).value,
        helsetilstandHindrerArbeid = get(HelseTable.helsetilstandHindrerArbeid)
    )
}

fun ResultRow.toAnnetRow(): AnnetRow {
    return AnnetRow(
        id = get(AnnetTable.id).value,
        andreForholdHindrerArbeid = getOrNull(AnnetTable.andreForholdHindrerArbeid)
    )
}

fun ResultRow.toOpplysningerMarkerRow(): OpplysningerMarkerRow {
    return OpplysningerMarkerRow(
        id = get(OpplysningerOmArbeidssoekerTable.id).value,
        opplysningerId = get(OpplysningerOmArbeidssoekerTable.opplysningerOmArbeidssoekerId),
        periodeId = get(PeriodeOpplysningerTable.periodeId)
    )
}

fun ResultRow.toPeriodeRow(): PeriodeRow {
    val avsluttetMetadataId = getOrNull(PeriodeTable.avsluttetId)
    return PeriodeRow(
        id = get(PeriodeTable.id).value,
        periodeId = get(PeriodeTable.periodeId),
        identitetsnummer = get(PeriodeTable.identitetsnummer),
        startet = toStartetMetadataRow(),
        avsluttet = avsluttetMetadataId?.let { toAvsluttetMetadataRow() }
    )
}

fun ResultRow.toPeriodeOpplysningerRow(): PeriodeOpplysningerRow {
    return PeriodeOpplysningerRow(
        periodeId = get(PeriodeOpplysningerTable.periodeId),
        opplysningerOmArbeidssoekerTableId = get(PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId)
    )
}

fun ResultRow.toProfileringRow(): ProfileringRow {
    return ProfileringRow(
        id = get(ProfileringTable.id).value,
        profileringId = get(ProfileringTable.profileringId),
        periodeId = get(ProfileringTable.periodeId),
        opplysningerOmArbeidssoekerId = get(ProfileringTable.opplysningerOmArbeidssoekerId),
        sendtInnAv = toMetadataRow(),
        profilertTil = get(ProfileringTable.profilertTil),
        jobbetSammenhengendeSeksAvTolvSisteManeder = get(ProfileringTable.jobbetSammenhengendeSeksAvTolvSisteManeder),
        alder = get(ProfileringTable.alder)
    )
}

fun ResultRow.toMetadataRow(): MetadataRow {
    val tidspunktFraKildeId = getOrNull(MetadataTable.tidspunktFraKildeId)
    return MetadataRow(
        id = get(MetadataTable.id).value,
        tidspunkt = get(MetadataTable.tidspunkt),
        utfoertAv = toBrukerRow(),
        kilde = get(MetadataTable.kilde),
        aarsak = get(MetadataTable.aarsak),
        tidspunktFraKilde = tidspunktFraKildeId?.let { toTidspunktFraKildeRow() }
    )
}

private fun ResultRow.toStartetMetadataRow(): MetadataRow {
    val tidspunktFraKildeId = getOrNull(StartetMetadataAlias[MetadataTable.tidspunktFraKildeId])
    return MetadataRow(
        id = get(StartetMetadataAlias[MetadataTable.id]).value,
        tidspunkt = get(StartetMetadataAlias[MetadataTable.tidspunkt]),
        utfoertAv = toStartetBrukerRow(),
        kilde = get(StartetMetadataAlias[MetadataTable.kilde]),
        aarsak = get(StartetMetadataAlias[MetadataTable.aarsak]),
        tidspunktFraKilde = tidspunktFraKildeId?.let { toStartetTidspunktFraKildeRow() }
    )
}

private fun ResultRow.toAvsluttetMetadataRow(): MetadataRow {
    val tidspunktFraKildeId = getOrNull(AvsluttetMetadataAlias[MetadataTable.tidspunktFraKildeId])
    return MetadataRow(
        id = get(AvsluttetMetadataAlias[MetadataTable.id]).value,
        tidspunkt = get(AvsluttetMetadataAlias[MetadataTable.tidspunkt]),
        utfoertAv = toAvsluttetBrukerRow(),
        kilde = get(AvsluttetMetadataAlias[MetadataTable.kilde]),
        aarsak = get(AvsluttetMetadataAlias[MetadataTable.aarsak]),
        tidspunktFraKilde = tidspunktFraKildeId?.let { toAvsluttetTidspunktFraKildeRow() }
    )
}

private fun ResultRow.toBrukerRow(): BrukerRow {
    return BrukerRow(
        id = get(BrukerTable.id).value,
        type = get(BrukerTable.type),
        brukerId = get(BrukerTable.brukerId)
    )
}

private fun ResultRow.toStartetBrukerRow(): BrukerRow {
    return BrukerRow(
        id = get(StartetBrukerAlias[BrukerTable.id]).value,
        type = get(StartetBrukerAlias[BrukerTable.type]),
        brukerId = get(StartetBrukerAlias[BrukerTable.brukerId])
    )
}

private fun ResultRow.toAvsluttetBrukerRow(): BrukerRow {
    return BrukerRow(
        id = get(AvsluttetBrukerAlias[BrukerTable.id]).value,
        type = get(AvsluttetBrukerAlias[BrukerTable.type]),
        brukerId = get(AvsluttetBrukerAlias[BrukerTable.brukerId])
    )
}

private fun ResultRow.toTidspunktFraKildeRow(): TidspunktFraKildeRow {
    return TidspunktFraKildeRow(
        id = get(TidspunktFraKildeTable.id).value,
        tidspunkt = get(TidspunktFraKildeTable.tidspunkt),
        avviksType = get(TidspunktFraKildeTable.avviksType)
    )
}

private fun ResultRow.toStartetTidspunktFraKildeRow(): TidspunktFraKildeRow {
    return TidspunktFraKildeRow(
        id = get(StartetTidspunktAlias[TidspunktFraKildeTable.id]).value,
        tidspunkt = get(StartetTidspunktAlias[TidspunktFraKildeTable.tidspunkt]),
        avviksType = get(StartetTidspunktAlias[TidspunktFraKildeTable.avviksType])
    )
}

private fun ResultRow.toAvsluttetTidspunktFraKildeRow(): TidspunktFraKildeRow {
    return TidspunktFraKildeRow(
        id = get(AvsluttetTidspunktAlias[TidspunktFraKildeTable.id]).value,
        tidspunkt = get(AvsluttetTidspunktAlias[TidspunktFraKildeTable.tidspunkt]),
        avviksType = get(AvsluttetTidspunktAlias[TidspunktFraKildeTable.avviksType])
    )
}
