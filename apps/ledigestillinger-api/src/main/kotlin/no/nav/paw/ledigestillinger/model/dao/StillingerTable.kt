package no.nav.paw.ledigestillinger.model.dao

import no.nav.paw.ledigestillinger.model.offset
import no.nav.paw.ledigestillinger.model.order
import no.nav.paw.ledigestillinger.model.size
import no.nav.paw.logging.logger.buildNamedLogger
import no.naw.paw.ledigestillinger.model.Fylke
import no.naw.paw.ledigestillinger.model.Paging
import no.naw.paw.ledigestillinger.model.StillingStatus
import no.naw.paw.ledigestillinger.model.VisningGrad
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.javatime.timestamp
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
}

fun StillingerTable.selectIdByUUID(
    uuid: UUID
): Long? = select(StillingerTable.id)
    .where { StillingerTable.uuid eq uuid }
    .map { it[StillingerTable.id].value }
    .singleOrNull()

fun StillingerTable.selectRowByUUID(
    uuid: UUID
): StillingRow? = selectAll()
    .where { StillingerTable.uuid eq uuid }
    .map {
        it.asStillingRow(
            arbeidsgiver = ArbeidsgivereTable::selectRowByParentId,
            kategorier = KategorierTable::selectRowsByParentId,
            klassifiseringer = KlassifiseringerTable::selectRowsByParentId,
            lokasjoner = LokasjonerTable::selectRowsByParentId,
            egenskaper = EgenskaperTable::selectRowsByParentId
        )
    }
    .singleOrNull()

fun StillingerTable.selectRowsByKategorierAndFylker(
    soekeord: Collection<String>, // TODO Benytt søkeord?
    kategorier: Collection<String>,
    fylker: Collection<Fylke>,
    paging: Paging = Paging()
): List<StillingRow> {
    val fylkesnummer = fylker.mapNotNull { it.fylkesnummer }
    val kommunenummer = fylker.flatMap { it.kommuner }.map { it.kommunenummer }

    val kategorierQuery: Op<Boolean> = KategorierTable.normalisertKode inList kategorier
    val fylkerQuery: Op<Boolean> = LokasjonerTable.fylkeskode inList fylkesnummer
    val kommunerQuery: Op<Boolean> = LokasjonerTable.kommunekode inList kommunenummer
    val aktivQuery: Op<Boolean> = StillingerTable.status eq StillingStatus.AKTIV

    val combinedQuery: Op<Boolean> = if (kategorier.isEmpty()) {
        if (fylkesnummer.isEmpty()) {
            if (kommunenummer.isEmpty()) {
                logger.debug("Query på aktive stillinger uten kategorier, fylker eller kommuner")
                aktivQuery
            } else {
                logger.debug("Query på aktive stillinger med kommuner, men uten kategorier eller fylker")
                kommunerQuery and aktivQuery
            }
        } else {
            if (kommunenummer.isEmpty()) {
                logger.debug("Query på aktive stillinger med fylker, men uten kategorier eller kommuner")
                fylkerQuery and aktivQuery
            } else {
                logger.debug("Query på aktive stillinger med fylker og kommuner, men uten kategorier")
                fylkerQuery and kommunerQuery and aktivQuery
            }
        }
    } else {
        if (fylkesnummer.isEmpty()) {
            if (kommunenummer.isEmpty()) {
                logger.debug("Query på aktive stillinger med kategorier, men uten fylker eller kommuner")
                kategorierQuery and aktivQuery
            } else {
                logger.debug("Query på aktive stillinger med kategorier og kommuner, men uten fylker")
                kategorierQuery and kommunerQuery and aktivQuery
            }
        } else {
            if (kommunenummer.isEmpty()) {
                logger.debug("Query på aktive stillinger med kategorier og fylker, men uten kommuner")
                kategorierQuery and fylkerQuery and aktivQuery
            } else {
                logger.debug("Query på aktive stillinger med kategorier, fylker og kommuner")
                kategorierQuery and fylkerQuery and kommunerQuery and aktivQuery
            }
        }
    }

    return join(KategorierTable, JoinType.LEFT, StillingerTable.id, KategorierTable.parentId)
        .join(LokasjonerTable, JoinType.LEFT, StillingerTable.id, LokasjonerTable.parentId)
        .selectAll()
        .where { combinedQuery }
        .orderBy(StillingerTable.publisertTimestamp, paging.order())
        .limit(paging.size()).offset(paging.offset())
        .map {
            it.asStillingRow(
                arbeidsgiver = ArbeidsgivereTable::selectRowByParentId,
                kategorier = KategorierTable::selectRowsByParentId,
                klassifiseringer = { emptyList() }, // TODO fjernet for å spare select KlassifiseringerTable::selectRowsByParentId,
                lokasjoner = LokasjonerTable::selectRowsByParentId,
                egenskaper = { emptyList() } // TODO fjernet for å spare select EgenskaperTable::selectRowsByParentId
            )
        }
}

fun StillingerTable.insert(
    row: StillingRow
): Long = insertAndGetId {
    it[this.uuid] = row.uuid
    it[this.adnr] = row.adnr
    it[this.tittel] = row.tittel
    it[this.status] = row.status
    it[this.visning] = row.visning
    it[this.kilde] = row.kilde
    it[this.medium] = row.medium
    it[this.referanse] = row.referanse
    it[this.arbeidsgivernavn] = row.arbeidsgivernavn
    it[this.stillingstittel] = row.stillingstittel
    it[this.ansettelsesform] = row.ansettelsesform
    it[this.stillingsprosent] = row.stillingsprosent
    it[this.stillingsantall] = row.stillingsantall
    it[this.sektor] = row.sektor
    it[this.soeknadsfrist] = row.soeknadsfrist
    it[this.oppstartsfrist] = row.oppstartsfrist
    it[this.opprettetTimestamp] = row.opprettetTimestamp
    it[this.endretTimestamp] = row.endretTimestamp
    it[this.publisertTimestamp] = row.publisertTimestamp
    it[this.utloeperTimestamp] = row.utloeperTimestamp
    it[this.messageTimestamp] = row.messageTimestamp
    it[this.insertTimestamp] = Instant.now()
}.value

fun StillingerTable.updateById(
    id: Long,
    row: StillingRow
): Int = update(
    where = { StillingerTable.id eq id }
) {
    it[this.adnr] = row.adnr
    it[this.tittel] = row.tittel
    it[this.status] = row.status
    it[this.visning] = row.visning
    it[this.kilde] = row.kilde
    it[this.medium] = row.medium
    it[this.referanse] = row.referanse
    it[this.arbeidsgivernavn] = row.arbeidsgivernavn
    it[this.stillingstittel] = row.stillingstittel
    it[this.ansettelsesform] = row.ansettelsesform
    it[this.stillingsprosent] = row.stillingsprosent
    it[this.stillingsantall] = row.stillingsantall
    it[this.sektor] = row.sektor
    it[this.soeknadsfrist] = row.soeknadsfrist
    it[this.oppstartsfrist] = row.oppstartsfrist
    it[this.opprettetTimestamp] = row.opprettetTimestamp
    it[this.endretTimestamp] = row.endretTimestamp
    it[this.publisertTimestamp] = row.publisertTimestamp
    it[this.utloeperTimestamp] = row.utloeperTimestamp
    it[this.messageTimestamp] = row.messageTimestamp
    it[this.updatedTimestamp] = Instant.now()
}
