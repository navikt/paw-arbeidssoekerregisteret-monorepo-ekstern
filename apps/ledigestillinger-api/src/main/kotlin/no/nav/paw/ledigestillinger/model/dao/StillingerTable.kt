package no.nav.paw.ledigestillinger.model.dao

import no.nav.paw.ledigestillinger.model.offset
import no.nav.paw.ledigestillinger.model.order
import no.nav.paw.ledigestillinger.model.size
import no.nav.paw.logging.logger.buildNamedLogger
import no.naw.paw.ledigestillinger.model.Fylke
import no.naw.paw.ledigestillinger.model.KlassifiseringType
import no.naw.paw.ledigestillinger.model.Paging
import no.naw.paw.ledigestillinger.model.StillingStatus
import no.naw.paw.ledigestillinger.model.VisningGrad
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.*

private val logger = buildNamedLogger("database")

object StillingerTable : LongIdTable("stillinger") {
    val uuid = uuid("uuid")
    val adnr = varchar("adnr", 50).nullable()
    val tittel = varchar("tittel", 1000)
    val status = enumerationByName<StillingStatus>("status", 20)
    val visning = enumerationByName<VisningGrad>("visning", 20)
    val kilde = varchar("kilde", 255)
    val medium = varchar("medium", 255)
    val referanse = varchar("referanse", 255)
    val arbeidsgivernavn = varchar("arbeidsgivernavn", 255).nullable()
    val stillingstittel = varchar("stillingstittel", 2000).nullable()
    val ansettelsesform = varchar("ansettelsesform", 255).nullable()
    val stillingsprosent = varchar("stillingsprosent", 20).nullable()
    val stillingsantall = varchar("stillingsantall", 20).nullable()
    val sektor = varchar("sektor", 50).nullable()
    val soeknadsfrist = varchar("soeknadsfrist", 255).nullable()
    val oppstartsfrist = varchar("oppstartsfrist", 255).nullable()
    val opprettetTimestamp = timestamp("opprettet_timestamp")
    val endretTimestamp = timestamp("endret_timestamp")
    val publisertTimestamp = timestamp("publisert_timestamp")
    val utloeperTimestamp = timestamp("utloeper_timestamp").nullable()
    val messageTimestamp = timestamp("message_timestamp")
    val insertTimestamp = timestamp("inserted_timestamp")
    val updatedTimestamp = timestamp("updated_timestamp").nullable()

    fun selectIdByUUID(
        uuid: UUID
    ): Long? = select(id)
        .where { StillingerTable.uuid eq uuid }
        .map { it[id].value }
        .singleOrNull()

    fun selectIdListByStatus(
        status: StillingStatus
    ): List<Long> = select(id)
        .where { StillingerTable.status eq status }
        .map { it[id].value }

    fun selectIdListByStatusListAndUtloeperGraterThan(
        statusList: Collection<StillingStatus>,
        utloeperTimestampCutoff: Instant
    ): List<Long> = select(id)
        .where { status inList statusList and utloeperTimestamp.isNotNull() and (utloeperTimestamp greater utloeperTimestampCutoff) }
        .map { it[id].value }

    fun selectRowByUUID(
        uuid: UUID
    ): StillingRow? = selectAll()
        .where { StillingerTable.uuid eq uuid }
        .map {
            it.asStillingRow(
                arbeidsgiver = ArbeidsgivereTable::selectRowByParentId,
                kategorier = KategorierTable::selectRowsByParentId,
                klassifiseringer = KlassifiseringerTable::selectRowsByParentId,
                lokasjoner = LokasjonerTable::selectRowsByParentId,
                egenskaper = { emptyList() } // TODO fjernet for å spare tid : EgenskaperTable::selectRowsByParentId
            )
        }
        .singleOrNull()

    fun selectRowsByUUIDList(
        uuidList: Collection<UUID>
    ): List<StillingRow> = selectAll()
        .where { uuid inList uuidList }
        .map {
            it.asStillingRow(
                arbeidsgiver = ArbeidsgivereTable::selectRowByParentId,
                kategorier = KategorierTable::selectRowsByParentId,
                klassifiseringer = KlassifiseringerTable::selectRowsByParentId,
                lokasjoner = LokasjonerTable::selectRowsByParentId,
                egenskaper = { emptyList() } // TODO fjernet for å spare tid : EgenskaperTable::selectRowsByParentId
            )
        }

    fun selectRowsByKategorierAndFylker(
        soekeord: Collection<String>,
        styrkkoder: Collection<String>,
        fylker: Collection<Fylke>,
        paging: Paging = Paging()
    ): List<StillingRow> {
        logger.trace("Finner stillinger med styrkkoder: {} og fylker: {}", styrkkoder, fylker)
        val aktivQuery: Op<Boolean> = (status eq StillingStatus.AKTIV)
        val soekeordQuery: Op<Boolean> = if (soekeord.isEmpty()) {
            Op.TRUE
        } else {
            Op.TRUE // TODO Benytte søkeord?
        }
        val kategoriQuery: Op<Boolean> = if (styrkkoder.isEmpty()) {
            Op.TRUE
        } else {
            (KategorierTable.normalisertKode inList styrkkoder)
        }
        val klassifiseringQuery: Op<Boolean> = if (styrkkoder.isEmpty()) {
            Op.TRUE
        } else {
            ((KlassifiseringerTable.type eq KlassifiseringType.STYRK08) and (KlassifiseringerTable.kode inList styrkkoder))
        }
        val styrkQuery = (kategoriQuery or klassifiseringQuery)
        val lokasjonQuery: Op<Boolean> = fylker.map { fylke ->
            if (fylke.kommuner.isEmpty()) {
                LokasjonerTable.fylkeskode eq fylke.fylkesnummer
            } else {
                val kommunenummer = fylke.kommuner.map { it.kommunenummer }
                LokasjonerTable.fylkeskode eq fylke.fylkesnummer and (LokasjonerTable.kommunekode inList kommunenummer)
            }
        }.reduceOrNull { aggregate, op -> aggregate or op } ?: Op.TRUE
        val kildeQuery = (kilde neq "DIR") // TODO Filtrerer ut direktemeldte stillinger

        val combinedQuery: Op<Boolean> = aktivQuery and styrkQuery and lokasjonQuery and soekeordQuery and kildeQuery

        return join(KategorierTable, JoinType.LEFT, id, KategorierTable.parentId)
            .join(KlassifiseringerTable, JoinType.LEFT, id, KlassifiseringerTable.parentId)
            .join(LokasjonerTable, JoinType.LEFT, id, LokasjonerTable.parentId)
            .selectAll()
            .where { combinedQuery }
            .orderBy(publisertTimestamp, paging.order())
            .limit(paging.size()).offset(paging.offset())
            .map {
                it.asStillingRow(
                    arbeidsgiver = ArbeidsgivereTable::selectRowByParentId,
                    kategorier = KategorierTable::selectRowsByParentId,
                    klassifiseringer = KlassifiseringerTable::selectRowsByParentId,
                    lokasjoner = LokasjonerTable::selectRowsByParentId,
                    egenskaper = { emptyList() } // TODO fjernet for å spare tid : EgenskaperTable::selectRowsByParentId
                )
            }
    }

    fun insert(
        row: StillingRow
    ): Long = insertAndGetId {
        it[uuid] = row.uuid
        it[adnr] = row.adnr
        it[tittel] = row.tittel
        it[status] = row.status
        it[visning] = row.visning
        it[kilde] = row.kilde
        it[medium] = row.medium
        it[referanse] = row.referanse
        it[arbeidsgivernavn] = row.arbeidsgivernavn
        it[stillingstittel] = row.stillingstittel
        it[ansettelsesform] = row.ansettelsesform
        it[stillingsprosent] = row.stillingsprosent
        it[stillingsantall] = row.stillingsantall
        it[sektor] = row.sektor
        it[soeknadsfrist] = row.soeknadsfrist
        it[oppstartsfrist] = row.oppstartsfrist
        it[opprettetTimestamp] = row.opprettetTimestamp
        it[endretTimestamp] = row.endretTimestamp
        it[publisertTimestamp] = row.publisertTimestamp
        it[utloeperTimestamp] = row.utloeperTimestamp
        it[messageTimestamp] = row.messageTimestamp
        it[insertTimestamp] = Instant.now()
    }.value

    fun updateById(
        id: Long,
        row: StillingRow
    ): Int = update(
        where = { StillingerTable.id eq id }
    ) {
        it[adnr] = row.adnr
        it[tittel] = row.tittel
        it[status] = row.status
        it[visning] = row.visning
        it[kilde] = row.kilde
        it[medium] = row.medium
        it[referanse] = row.referanse
        it[arbeidsgivernavn] = row.arbeidsgivernavn
        it[stillingstittel] = row.stillingstittel
        it[ansettelsesform] = row.ansettelsesform
        it[stillingsprosent] = row.stillingsprosent
        it[stillingsantall] = row.stillingsantall
        it[sektor] = row.sektor
        it[soeknadsfrist] = row.soeknadsfrist
        it[oppstartsfrist] = row.oppstartsfrist
        it[opprettetTimestamp] = row.opprettetTimestamp
        it[endretTimestamp] = row.endretTimestamp
        it[publisertTimestamp] = row.publisertTimestamp
        it[utloeperTimestamp] = row.utloeperTimestamp
        it[messageTimestamp] = row.messageTimestamp
        it[updatedTimestamp] = Instant.now()
    }

    fun deleteByIdList(
        idList: Collection<Long>
    ): Int = deleteWhere { id inList idList }

    fun countByStatus(): List<StillingStatusCountRow> = select(
        status, status.count()
    ).groupBy(status).map {
        StillingStatusCountRow(
            status = it[status],
            count = it[status.count()]
        )
    }
}
