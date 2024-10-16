package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.AnnetTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BeskrivelseMedDetaljerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BeskrivelseTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.BrukerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.DetaljerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.HelseTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.MetadataTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.OpplysningerOmArbeidssoekerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeOpplysningerTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.ProfileringTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.TidspunktFraKildeTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.UtdanningTable
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BeskrivelseMedDetaljerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.MetadataRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.TidspunktFraKildeRow
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import java.util.*

val StartetMetadataAlias = MetadataTable.alias("startet_metadata")
val AvsluttetMetadataAlias = MetadataTable.alias("avsluttet_metadata")
val StartetBrukerAlias = BrukerTable.alias("startet_bruker")
val AvsluttetBrukerAlias = BrukerTable.alias("avsluttet_bruker")
val StartetTidspunktAlias = TidspunktFraKildeTable.alias("startet_tidspunkt")
val AvsluttetTidspunktAlias = TidspunktFraKildeTable.alias("avsluttet_tidspunkt")

// ### OPPLYSNINGER ###

fun Transaction.finnOpplysninger(periodeId: UUID): List<OpplysningerOmArbeidssoekerResponse> {
    return OpplysningerOmArbeidssoekerTable
        .join(MetadataTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.sendtInnAvId, MetadataTable.id)
        .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
        .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
        .join(UtdanningTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.utdanningId, UtdanningTable.id)
        .join(HelseTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.helseId, HelseTable.id)
        .join(AnnetTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.annetId, AnnetTable.id)
        .join(
            PeriodeOpplysningerTable,
            JoinType.LEFT,
            OpplysningerOmArbeidssoekerTable.id,
            PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId
        )
        .selectAll()
        .where { PeriodeOpplysningerTable.periodeId eq periodeId }
        .mapNotNull {
            val opplysningerOmArbeidssoekerId = it[OpplysningerOmArbeidssoekerTable.id].value
            val beskrivelseMedDetaljer = finnBeskrivelseMedDetaljer(opplysningerOmArbeidssoekerId)
            it.toOpplysningerResponse(beskrivelseMedDetaljer)
        }
}

fun Transaction.finnOpplysninger(identitetsnummer: Identitetsnummer): List<OpplysningerOmArbeidssoekerResponse> {
    return OpplysningerOmArbeidssoekerTable
        .join(MetadataTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.sendtInnAvId, MetadataTable.id)
        .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
        .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
        .join(UtdanningTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.utdanningId, UtdanningTable.id)
        .join(HelseTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.helseId, HelseTable.id)
        .join(AnnetTable, JoinType.LEFT, OpplysningerOmArbeidssoekerTable.annetId, AnnetTable.id)
        .join(
            PeriodeOpplysningerTable,
            JoinType.LEFT,
            OpplysningerOmArbeidssoekerTable.id,
            PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId
        )
        .join(PeriodeTable, JoinType.LEFT, PeriodeOpplysningerTable.periodeId, PeriodeTable.periodeId)
        .selectAll()
        .where { PeriodeTable.identitetsnummer eq identitetsnummer.verdi }
        .mapNotNull {
            val opplysningerOmArbeidssoekerId = it[OpplysningerOmArbeidssoekerTable.id].value
            val beskrivelseMedDetaljer = finnBeskrivelseMedDetaljer(opplysningerOmArbeidssoekerId)
            it.toOpplysningerResponse(beskrivelseMedDetaljer)
        }
}

private fun Transaction.finnBeskrivelseMedDetaljer(opplysningerOmArbeidssoekerId: Long): List<BeskrivelseMedDetaljerResponse> {
    return BeskrivelseMedDetaljerTable
        .join(
            BeskrivelseTable,
            JoinType.LEFT,
            BeskrivelseMedDetaljerTable.id,
            BeskrivelseTable.beskrivelseMedDetaljerId
        )
        .selectAll()
        .where { BeskrivelseMedDetaljerTable.opplysningerOmArbeidssoekerId eq opplysningerOmArbeidssoekerId }
        .mapNotNull {
            val beskrivelseId = it[BeskrivelseTable.id].value
            val detaljer = hentDetaljer(beskrivelseId)
            it.toBeskrivelseMedDetaljerResponse(detaljer)
        }
}

private fun Transaction.hentDetaljer(beskrivelseId: Long): Map<String, String> {
    return DetaljerTable.selectAll().where { DetaljerTable.beskrivelseId eq beskrivelseId }
        .associate { detaljerResultRow ->
            detaljerResultRow[DetaljerTable.noekkel] to detaljerResultRow[DetaljerTable.verdi]
        }
}

fun Transaction.opprettOpplysninger(opplysninger: OpplysningerOmArbeidssoeker): Long {
    val id =
        OpplysningerOmArbeidssoekerTable.insertAndGetId {
            it[opplysningerOmArbeidssoekerId] = opplysninger.id
            it[sendtInnAvId] = opprettMetadata(opplysninger.sendtInnAv)
            it[utdanningId] = opplysninger.utdanning?.let { data -> opprettUtdanning(data) }
            it[helseId] = opplysninger.helse?.let { data -> opprettHelse(data) }
            it[annetId] = opplysninger.annet?.let { data -> opprettAnnet(data) }
        }.value
    opplysninger.jobbsituasjon.beskrivelser.forEach { beskrivelseMedDetaljer ->
        val beskrivelseMedDetaljerId = opprettBeskrivelseMedDetaljer(id)
        val beskrivelserId = opprettBeskrivelse(beskrivelseMedDetaljer.beskrivelse, beskrivelseMedDetaljerId)
        beskrivelseMedDetaljer.detaljer.forEach { detalj ->
            opprettDetaljer(beskrivelserId, detalj)
        }
    }
    opprettPeriodeOpplysninger(id, opplysninger.periodeId)
    return id
}

private fun Transaction.opprettUtdanning(utdanning: Utdanning): Long =
    UtdanningTable.insertAndGetId {
        it[nus] = utdanning.nus
        it[bestaatt] = utdanning.bestaatt?.let { bestaatt -> JaNeiVetIkke.valueOf(bestaatt.name) }
        it[godkjent] = utdanning.godkjent?.let { godkjent -> JaNeiVetIkke.valueOf(godkjent.name) }
    }.value

private fun Transaction.opprettHelse(helse: Helse): Long =
    HelseTable.insertAndGetId {
        it[helsetilstandHindrerArbeid] = JaNeiVetIkke.valueOf(helse.helsetilstandHindrerArbeid.name)
    }.value

private fun Transaction.opprettAnnet(annet: Annet): Long =
    AnnetTable.insertAndGetId {
        it[andreForholdHindrerArbeid] = annet.andreForholdHindrerArbeid?.let { data -> JaNeiVetIkke.valueOf(data.name) }
    }.value

private fun Transaction.opprettBeskrivelseMedDetaljer(opplysningerOmArbeidssoekerId: Long): Long =
    BeskrivelseMedDetaljerTable.insertAndGetId {
        it[BeskrivelseMedDetaljerTable.opplysningerOmArbeidssoekerId] = opplysningerOmArbeidssoekerId
    }.value

private fun Transaction.opprettBeskrivelse(
    beskrivelse: Beskrivelse,
    beskrivelseMedDetaljerId: Long
): Long =
    BeskrivelseTable.insertAndGetId {
        it[BeskrivelseTable.beskrivelse] = Beskrivelse.valueOf(beskrivelse.name)
        it[BeskrivelseTable.beskrivelseMedDetaljerId] = beskrivelseMedDetaljerId
    }.value

private fun Transaction.opprettDetaljer(
    beskrivelseId: Long,
    detaljer: Map.Entry<String, String>
) {
    DetaljerTable.insert {
        it[DetaljerTable.beskrivelseId] = beskrivelseId
        it[noekkel] = detaljer.key
        it[verdi] = detaljer.value
    }
}

fun Transaction.opprettPeriodeOpplysninger(
    opplysningerOmArbeidssoekerId: Long,
    periodeId: UUID
) {
    PeriodeOpplysningerTable.insert {
        it[PeriodeOpplysningerTable.periodeId] = periodeId
        it[opplysningerOmArbeidssoekerTableId] = opplysningerOmArbeidssoekerId
    }
}

fun Transaction.finnOpplysningerRow(opplysningerId: UUID): OpplysningerRow? {
    return OpplysningerOmArbeidssoekerTable
        .join(
            PeriodeOpplysningerTable,
            JoinType.LEFT,
            OpplysningerOmArbeidssoekerTable.id,
            PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId
        )
        .selectAll()
        .where { OpplysningerOmArbeidssoekerTable.opplysningerOmArbeidssoekerId eq opplysningerId }
        .singleOrNull()?.toOpplysningerRow()
}

fun Transaction.finnOpplysningerRows(opplysningerIdList: List<UUID>): List<OpplysningerRow> {
    return OpplysningerOmArbeidssoekerTable
        .join(
            PeriodeOpplysningerTable,
            JoinType.LEFT,
            OpplysningerOmArbeidssoekerTable.id,
            PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId
        )
        .selectAll()
        .where { OpplysningerOmArbeidssoekerTable.opplysningerOmArbeidssoekerId inList opplysningerIdList }
        .map { it.toOpplysningerRow() }
}

fun Transaction.finnOpplysningerRows() =
    OpplysningerOmArbeidssoekerTable
        .join(
            PeriodeOpplysningerTable,
            JoinType.LEFT,
            OpplysningerOmArbeidssoekerTable.id,
            PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId
        )
        .selectAll()
        .map { it.toOpplysningerRow() }

// ### PERIODE ###

fun Transaction.finnPeriode(periodeId: UUID): PeriodeRow? {
    return PeriodeTable
        .join(StartetMetadataAlias, JoinType.LEFT, PeriodeTable.startetId, StartetMetadataAlias[MetadataTable.id])
        .join(AvsluttetMetadataAlias, JoinType.LEFT, PeriodeTable.avsluttetId, AvsluttetMetadataAlias[MetadataTable.id])
        .join(
            StartetBrukerAlias,
            JoinType.LEFT,
            StartetMetadataAlias[MetadataTable.utfoertAvId],
            StartetBrukerAlias[BrukerTable.id]
        )
        .join(
            AvsluttetBrukerAlias,
            JoinType.LEFT,
            AvsluttetMetadataAlias[MetadataTable.utfoertAvId],
            AvsluttetBrukerAlias[BrukerTable.id]
        )
        .join(
            StartetTidspunktAlias,
            JoinType.LEFT,
            StartetMetadataAlias[MetadataTable.tidspunktFraKildeId],
            StartetTidspunktAlias[TidspunktFraKildeTable.id]
        )
        .join(
            AvsluttetTidspunktAlias,
            JoinType.LEFT,
            AvsluttetMetadataAlias[MetadataTable.tidspunktFraKildeId],
            AvsluttetTidspunktAlias[TidspunktFraKildeTable.id]
        )
        .selectAll()
        .where { PeriodeTable.periodeId eq periodeId }.singleOrNull()?.toPeriodeRow()
}

fun Transaction.finnPerioderForIdentiteter(identitetsnummerList: List<Identitetsnummer>): List<PeriodeRow> {
    val identer = identitetsnummerList.map { it.verdi }
    return PeriodeTable
        .join(StartetMetadataAlias, JoinType.LEFT, PeriodeTable.startetId, StartetMetadataAlias[MetadataTable.id])
        .join(AvsluttetMetadataAlias, JoinType.LEFT, PeriodeTable.avsluttetId, AvsluttetMetadataAlias[MetadataTable.id])
        .join(
            StartetBrukerAlias,
            JoinType.LEFT,
            StartetMetadataAlias[MetadataTable.utfoertAvId],
            StartetBrukerAlias[BrukerTable.id]
        )
        .join(
            AvsluttetBrukerAlias,
            JoinType.LEFT,
            AvsluttetMetadataAlias[MetadataTable.utfoertAvId],
            AvsluttetBrukerAlias[BrukerTable.id]
        )
        .join(
            StartetTidspunktAlias,
            JoinType.LEFT,
            StartetMetadataAlias[MetadataTable.tidspunktFraKildeId],
            StartetTidspunktAlias[TidspunktFraKildeTable.id]
        )
        .join(
            AvsluttetTidspunktAlias,
            JoinType.LEFT,
            AvsluttetMetadataAlias[MetadataTable.tidspunktFraKildeId],
            AvsluttetTidspunktAlias[TidspunktFraKildeTable.id]
        )
        .selectAll()
        .where { PeriodeTable.identitetsnummer inList identer }
        .map { it.toPeriodeRow() }
}

fun Transaction.finnPerioder(periodeIdList: List<UUID>): List<PeriodeRow> {
    return PeriodeTable
        .join(StartetMetadataAlias, JoinType.LEFT, PeriodeTable.startetId, StartetMetadataAlias[MetadataTable.id])
        .join(AvsluttetMetadataAlias, JoinType.LEFT, PeriodeTable.avsluttetId, AvsluttetMetadataAlias[MetadataTable.id])
        .join(
            StartetBrukerAlias,
            JoinType.LEFT,
            StartetMetadataAlias[MetadataTable.utfoertAvId],
            StartetBrukerAlias[BrukerTable.id]
        )
        .join(
            AvsluttetBrukerAlias,
            JoinType.LEFT,
            AvsluttetMetadataAlias[MetadataTable.utfoertAvId],
            AvsluttetBrukerAlias[BrukerTable.id]
        )
        .join(
            StartetTidspunktAlias,
            JoinType.LEFT,
            StartetMetadataAlias[MetadataTable.tidspunktFraKildeId],
            StartetTidspunktAlias[TidspunktFraKildeTable.id]
        )
        .join(
            AvsluttetTidspunktAlias,
            JoinType.LEFT,
            AvsluttetMetadataAlias[MetadataTable.tidspunktFraKildeId],
            AvsluttetTidspunktAlias[TidspunktFraKildeTable.id]
        )
        .selectAll()
        .where { PeriodeTable.periodeId inList periodeIdList }
        .map { it.toPeriodeRow() }
}

fun Transaction.opprettPeriode(periode: Periode) {
    PeriodeTable.insert {
        it[periodeId] = periode.id
        it[identitetsnummer] = periode.identitetsnummer
        it[startetId] = opprettMetadata(periode.startet)
        it[avsluttetId] = periode.avsluttet?.let { metadata -> opprettMetadata(metadata) }
    }
}

fun Transaction.oppdaterPeriode(periode: Periode, eksisterendePeriode: PeriodeRow) {
    oppdaterMetadata(periode.startet, eksisterendePeriode.startet)

    if (eksisterendePeriode.avsluttet != null) {
        if (periode.avsluttet == null) {
            PeriodeTable.update(where = { PeriodeTable.id eq eksisterendePeriode.id }) {
                it[avsluttetId] = null // Sletter referanse til avsluttet-metadata. Det vil føre til
            }
        } else {
            oppdaterMetadata(periode.avsluttet, eksisterendePeriode.avsluttet)
        }
    } else {
        if (periode.avsluttet != null) {
            PeriodeTable.update(where = { PeriodeTable.id eq eksisterendePeriode.id }) {
                it[avsluttetId] = opprettMetadata(periode.avsluttet)
            }
        }
    }
}

fun Transaction.finnPeriodeOpplysningerRows(periodeId: UUID) =
    PeriodeOpplysningerTable.selectAll()
        .where { PeriodeOpplysningerTable.periodeId eq periodeId }
        .map { it.toPeriodeOpplysningerRow() }

fun Transaction.finnPeriodeOpplysningerRows() =
    PeriodeOpplysningerTable.selectAll()
        .map { it.toPeriodeOpplysningerRow() }

// ### PROFILERING ###

fun Transaction.finnProfileringer(periodeId: UUID): List<ProfileringResponse> {
    return ProfileringTable
        .join(MetadataTable, JoinType.LEFT, ProfileringTable.sendtInnAvId, MetadataTable.id)
        .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
        .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
        .selectAll().where { ProfileringTable.periodeId eq periodeId }.map { it.toProfileringResponse() }
}

fun Transaction.finnProfileringer(identitetsnummerList: List<Identitetsnummer>): List<ProfileringResponse> {
    val identer = identitetsnummerList.map { it.verdi }
    return ProfileringTable
        .join(MetadataTable, JoinType.LEFT, ProfileringTable.sendtInnAvId, MetadataTable.id)
        .join(BrukerTable, JoinType.LEFT, MetadataTable.utfoertAvId, BrukerTable.id)
        .join(TidspunktFraKildeTable, JoinType.LEFT, MetadataTable.tidspunktFraKildeId, TidspunktFraKildeTable.id)
        .join(PeriodeTable, JoinType.LEFT, ProfileringTable.periodeId, PeriodeTable.periodeId)
        .selectAll().where { PeriodeTable.identitetsnummer inList identer }.map { it.toProfileringResponse() }
}

fun Transaction.opprettProfilering(profilering: Profilering) {
    ProfileringTable.insert {
        it[profileringId] = profilering.id
        it[periodeId] = profilering.periodeId
        it[opplysningerOmArbeidssoekerId] = profilering.opplysningerOmArbeidssokerId
        it[sendtInnAvId] = opprettMetadata(profilering.sendtInnAv)
        it[profilertTil] = profilering.profilertTil
        it[jobbetSammenhengendeSeksAvTolvSisteManeder] = profilering.jobbetSammenhengendeSeksAvTolvSisteMnd
        it[alder] = profilering.alder
    }
}

// ### FELLES ###

private fun Transaction.opprettMetadata(metadata: Metadata): Long {
    return MetadataTable.insertAndGetId {
        it[utfoertAvId] = opprettEllerOppdaterBruker(metadata.utfoertAv)
        it[tidspunkt] = metadata.tidspunkt
        it[kilde] = metadata.kilde
        it[aarsak] = metadata.aarsak
        it[tidspunktFraKildeId] = metadata.tidspunktFraKilde?.let { tidspunkt -> opprettTidspunktFraKilde(tidspunkt) }
    }.value
}

private fun Transaction.oppdaterMetadata(metadata: Metadata, eksisterendeMetadata: MetadataRow) {
    MetadataTable.update(where = { MetadataTable.id eq eksisterendeMetadata.id }) {
        it[utfoertAvId] = opprettEllerOppdaterBruker(metadata.utfoertAv)
        it[tidspunkt] = metadata.tidspunkt
        it[kilde] = metadata.kilde
        it[aarsak] = metadata.aarsak
        it[tidspunktFraKildeId] =
            opprettEllerOppdatereTidspunktFraKilde(metadata.tidspunktFraKilde, eksisterendeMetadata.tidspunktFraKilde)
    }
}

private fun Transaction.opprettEllerOppdaterBruker(bruker: Bruker): Long {
    val result = BrukerTable.upsert(
        BrukerTable.type,
        BrukerTable.brukerId,
        where = { (BrukerTable.type eq bruker.type) and (BrukerTable.brukerId eq bruker.id) }
    ) {
        it[type] = bruker.type
        it[brukerId] = bruker.id
    }.resultedValues?.singleOrNull() ?: throw IllegalStateException("Upsert-operasjon returnerte ingen resultat")
    return result[BrukerTable.id].value
}

private fun Transaction.opprettEllerOppdatereTidspunktFraKilde(
    tidspunktFraKilde: TidspunktFraKilde?,
    eksisterendeTidspunktFraKilde: TidspunktFraKildeRow?
): Long? {
    return if (tidspunktFraKilde == null) {
        null
    } else {
        if (eksisterendeTidspunktFraKilde == null) {
            opprettTidspunktFraKilde(tidspunktFraKilde)
        } else {
            oppdatereTidspunktFraKilde(tidspunktFraKilde, eksisterendeTidspunktFraKilde)
        }
    }
}

private fun Transaction.opprettTidspunktFraKilde(tidspunktFraKilde: TidspunktFraKilde): Long {
    return TidspunktFraKildeTable.insertAndGetId {
        it[tidspunkt] = tidspunktFraKilde.tidspunkt
        it[avviksType] = tidspunktFraKilde.avviksType
    }.value
}

private fun Transaction.oppdatereTidspunktFraKilde(
    tidspunktFraKilde: TidspunktFraKilde,
    eksisterendeTidspunktFraKilde: TidspunktFraKildeRow
): Long {
    TidspunktFraKildeTable.update(where = { TidspunktFraKildeTable.id eq eksisterendeTidspunktFraKilde.id }) {
        it[tidspunkt] = tidspunktFraKilde.tidspunkt
        it[avviksType] = tidspunktFraKilde.avviksType
    }
    return eksisterendeTidspunktFraKilde.id
}
