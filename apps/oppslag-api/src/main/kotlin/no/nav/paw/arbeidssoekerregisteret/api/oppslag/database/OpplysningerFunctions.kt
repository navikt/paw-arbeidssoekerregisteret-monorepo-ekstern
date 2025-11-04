package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BeskrivelseMedDetaljerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.DetaljerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerMarkerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import no.nav.paw.felles.model.Identitetsnummer
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.*

object OpplysningerFunctions {

    fun findForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<OpplysningerRow> {
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
            .where { PeriodeOpplysningerTable.periodeId inList periodeIdList }
            //.orderBy(MetadataTable.tidspunkt, paging.ordering)
            //.limit(paging.size).offset(paging.offset)
            .mapNotNull {
                val opplysningerId = it[OpplysningerOmArbeidssoekerTable.id].value
                val jobbsituasjon = findJobbsituasjon(opplysningerId)
                it.toOpplysningerRow(jobbsituasjon)
            }
    }

    fun findForIdentitetsnummerList(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<OpplysningerRow> {
        val identiteter = identitetsnummerList.map { it.verdi }
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
            .where { PeriodeTable.identitetsnummer inList identiteter }
            //.orderBy(MetadataTable.tidspunkt, paging.ordering)
            //.limit(paging.size).offset(paging.offset)
            .mapNotNull {
                val opplysningerId = it[OpplysningerOmArbeidssoekerTable.id].value
                val jobbsituasjon = findJobbsituasjon(opplysningerId)
                it.toOpplysningerRow(jobbsituasjon)
            }
    }

    private fun findJobbsituasjon(opplysningerId: Long): List<BeskrivelseMedDetaljerRow> {
        return BeskrivelseMedDetaljerTable
            .join(
                BeskrivelseTable,
                JoinType.LEFT,
                BeskrivelseMedDetaljerTable.id,
                BeskrivelseTable.beskrivelseMedDetaljerId
            )
            .selectAll()
            .where { BeskrivelseMedDetaljerTable.opplysningerOmArbeidssoekerId eq opplysningerId }
            .mapNotNull {
                val beskrivelseId = it[BeskrivelseTable.id].value
                val detaljer = findDetaljer(beskrivelseId)
                it.toBeskrivelseMedDetaljerRow(detaljer)
            }
    }

    private fun findDetaljer(beskrivelseId: Long): List<DetaljerRow> {
        return DetaljerTable
            .selectAll()
            .where { DetaljerTable.beskrivelseId eq beskrivelseId }
            .map { it.toDetaljerRow() }
    }

    fun insert(opplysninger: OpplysningerOmArbeidssoeker): Long {
        val id =
            OpplysningerOmArbeidssoekerTable.insertAndGetId {
                it[opplysningerOmArbeidssoekerId] = opplysninger.id
                it[sendtInnAvId] = MetadataFunctions.insert(opplysninger.sendtInnAv)
                it[utdanningId] = opplysninger.utdanning?.let { data -> insert(data) }
                it[helseId] = opplysninger.helse?.let { data -> insert(data) }
                it[annetId] = opplysninger.annet?.let { data -> insert(data) }
            }.value
        opplysninger.jobbsituasjon.beskrivelser.forEach { beskrivelseMedDetaljer ->
            val beskrivelseMedDetaljerId = insert(id)
            val beskrivelserId = insert(beskrivelseMedDetaljer.beskrivelse, beskrivelseMedDetaljerId)
            beskrivelseMedDetaljer.detaljer.forEach { detalj -> insert(beskrivelserId, detalj) }
        }
        insert(id, opplysninger.periodeId)
        return id
    }

    private fun insert(utdanning: Utdanning): Long =
        UtdanningTable.insertAndGetId {
            it[nus] = utdanning.nus
            it[bestaatt] = utdanning.bestaatt?.let { bestaatt -> JaNeiVetIkke.valueOf(bestaatt.name) }
            it[godkjent] = utdanning.godkjent?.let { godkjent -> JaNeiVetIkke.valueOf(godkjent.name) }
        }.value

    private fun insert(helse: Helse): Long =
        HelseTable.insertAndGetId {
            it[helsetilstandHindrerArbeid] = JaNeiVetIkke.valueOf(helse.helsetilstandHindrerArbeid.name)
        }.value

    private fun insert(annet: Annet): Long =
        AnnetTable.insertAndGetId {
            it[andreForholdHindrerArbeid] =
                annet.andreForholdHindrerArbeid?.let { data -> JaNeiVetIkke.valueOf(data.name) }
        }.value

    private fun insert(opplysningerOmArbeidssoekerId: Long): Long =
        BeskrivelseMedDetaljerTable.insertAndGetId {
            it[BeskrivelseMedDetaljerTable.opplysningerOmArbeidssoekerId] = opplysningerOmArbeidssoekerId
        }.value

    private fun insert(
        beskrivelse: Beskrivelse,
        beskrivelseMedDetaljerId: Long
    ): Long =
        BeskrivelseTable.insertAndGetId {
            it[BeskrivelseTable.beskrivelse] = Beskrivelse.valueOf(beskrivelse.name)
            it[BeskrivelseTable.beskrivelseMedDetaljerId] = beskrivelseMedDetaljerId
        }.value

    private fun insert(
        beskrivelseId: Long,
        detaljer: Map.Entry<String, String>
    ) {
        DetaljerTable.insert {
            it[DetaljerTable.beskrivelseId] = beskrivelseId
            it[noekkel] = detaljer.key
            it[verdi] = detaljer.value
        }
    }

    private fun insert(
        opplysningerOmArbeidssoekerId: Long,
        periodeId: UUID
    ) {
        PeriodeOpplysningerTable.insert {
            it[PeriodeOpplysningerTable.periodeId] = periodeId
            it[opplysningerOmArbeidssoekerTableId] = opplysningerOmArbeidssoekerId
        }
    }

    fun getForOpplysningerId(opplysningerId: UUID): OpplysningerMarkerRow? {
        return OpplysningerOmArbeidssoekerTable
            .join(
                PeriodeOpplysningerTable,
                JoinType.LEFT,
                OpplysningerOmArbeidssoekerTable.id,
                PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId
            )
            .selectAll()
            .where { OpplysningerOmArbeidssoekerTable.opplysningerOmArbeidssoekerId eq opplysningerId }
            .singleOrNull()?.toOpplysningerMarkerRow()
    }

    fun finnRows() =
        OpplysningerOmArbeidssoekerTable
            .join(
                PeriodeOpplysningerTable,
                JoinType.LEFT,
                OpplysningerOmArbeidssoekerTable.id,
                PeriodeOpplysningerTable.opplysningerOmArbeidssoekerTableId
            )
            .selectAll()
            .map { it.toOpplysningerMarkerRow() }

}
