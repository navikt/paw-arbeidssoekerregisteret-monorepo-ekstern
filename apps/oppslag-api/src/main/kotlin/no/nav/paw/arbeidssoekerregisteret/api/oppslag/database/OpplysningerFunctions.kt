package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BeskrivelseMedDetaljerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerRow
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import java.util.*

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
