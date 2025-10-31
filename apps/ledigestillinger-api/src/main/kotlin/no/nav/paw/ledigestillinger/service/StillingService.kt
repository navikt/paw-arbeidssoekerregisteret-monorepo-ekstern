package no.nav.paw.ledigestillinger.service

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.config.ApplicationConfig
import no.nav.paw.ledigestillinger.exception.StillingIkkeFunnetException
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.asStillingRow
import no.nav.paw.ledigestillinger.model.dao.ArbeidsgivereTable
import no.nav.paw.ledigestillinger.model.dao.EgenskaperTable
import no.nav.paw.ledigestillinger.model.dao.KategorierTable
import no.nav.paw.ledigestillinger.model.dao.KlassifiseringerTable
import no.nav.paw.ledigestillinger.model.dao.LokasjonerTable
import no.nav.paw.ledigestillinger.model.dao.StillingRow
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.util.finnStillingerByEgenskaperEvent
import no.nav.paw.ledigestillinger.util.finnStillingerByEgenskaperGauge
import no.nav.paw.ledigestillinger.util.finnStillingerByUuidListeEvent
import no.nav.paw.ledigestillinger.util.finnStillingerByUuidListeGauge
import no.nav.paw.ledigestillinger.util.fromLocalDateTimeString
import no.nav.paw.ledigestillinger.util.meldingerMottattCounter
import no.nav.paw.ledigestillinger.util.meldingerMottattEvent
import no.nav.paw.ledigestillinger.util.meldingerMottattGauge
import no.nav.paw.logging.logger.buildLogger
import no.naw.paw.ledigestillinger.model.Fylke
import no.naw.paw.ledigestillinger.model.Paging
import no.naw.paw.ledigestillinger.model.Stilling
import no.naw.paw.ledigestillinger.model.StillingStatus
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.*

class StillingService(
    private val applicationConfig: ApplicationConfig,
    private val meterRegistry: MeterRegistry
) {
    private val logger = buildLogger

    init {
        logger.info(
            "Starter konsumering av stillinger som er publisert etter {}",
            applicationConfig.velgStillingerNyereEnn
        )
    }

    @WithSpan
    fun hentStilling(uuid: UUID): Stilling = transaction {
        logger.info("Henter stilling")
        val row = StillingerTable.selectRowByUUID(uuid)
        row?.asDto() ?: throw StillingIkkeFunnetException()
    }

    @WithSpan
    fun finnStillingerByUuidListe(
        uuidListe: Collection<UUID>
    ): List<Stilling> = transaction {
        logger.info("Finner stillinger for UUID-liste")
        Span.current().finnStillingerByUuidListeEvent(antallUuider = uuidListe.size)
        meterRegistry.finnStillingerByUuidListeGauge(antallUuider = uuidListe.size)
        val rows = StillingerTable.selectRowsByUUIDList(uuidListe)
        rows.map { it.asDto() }
    }

    @WithSpan
    fun finnStillingerByEgenskaper(
        soekeord: Collection<String>,
        kategorier: Collection<String>,
        fylker: Collection<Fylke>,
        paging: Paging = Paging()
    ): List<Stilling> = transaction {
        logger.info("Finner stillinger for egeneskaper")
        Span.current().finnStillingerByEgenskaperEvent(
            antallSoekeord = soekeord.size,
            antallKategorier = kategorier.size,
            antallFylker = fylker.size,
            antallKommuner = fylker.sumOf { it.kommuner.size }
        )
        meterRegistry.finnStillingerByEgenskaperGauge(
            antallSoekeord = soekeord.size,
            antallKategorier = kategorier.size,
            antallFylker = fylker.size,
            antallKommuner = fylker.sumOf { it.kommuner.size }
        )
        val rows = StillingerTable.selectRowsByKategorierAndFylker(
            soekeord = soekeord,
            kategorier = kategorier,
            fylker = fylker,
            paging = paging
        )
        rows.map { it.asDto() }
    }

    @WithSpan
    fun lagreStilling(stillingRow: StillingRow) {
        logger.debug("Lagrer stilling")
        val existingId = StillingerTable.selectIdByUUID(stillingRow.uuid)
        if (existingId == null) {
            val id = StillingerTable.insert(stillingRow)
            stillingRow.arbeidsgiver?.let { row ->
                ArbeidsgivereTable.insert(
                    parentId = id,
                    row = row
                )
            }
            KategorierTable.insert(
                parentId = id,
                rows = stillingRow.kategorier
            )
            KlassifiseringerTable.insert(
                parentId = id,
                rows = stillingRow.klassifiseringer
            )
            LokasjonerTable.insert(
                parentId = id,
                rows = stillingRow.lokasjoner
            )
            EgenskaperTable.insert(
                parentId = id,
                rows = stillingRow.egenskaper
            )
        } else {
            StillingerTable.updateById(
                id = existingId,
                row = stillingRow
            )
        }
    }

    fun slettStillinger(medUtloeperEldreEnn: Duration): Int = transaction {
        val utloeperTimestampCutoff = Instant.now().minus(medUtloeperEldreEnn)
        logger.info("Sletter stillinger med utløp eldre enn {}", utloeperTimestampCutoff)
        val slettetIdList = StillingerTable.selectIdListByStatus(status = StillingStatus.SLETTET)
        val idList = slettetIdList + StillingerTable.selectIdListByStatusListAndUtloeperGraterThan(
            statusList = listOf(StillingStatus.AVVIST, StillingStatus.INAKTIV, StillingStatus.STOPPET),
            utloeperTimestampCutoff = utloeperTimestampCutoff
        )
        StillingerTable.deleteByIdList(idList)
            .also { rowsAffected -> logger.info("Slettet {} stillinger", rowsAffected) }
    }

    @WithSpan
    fun handleMessages(messages: Sequence<Message<UUID, Ad>>): Unit = transaction {
        var antallMottatt = 0
        var antallLagret = 0
        val start = System.currentTimeMillis()
        messages
            .onEach { message ->
                antallMottatt++
                logger.trace("Mottatt melding på topic=${message.topic}, partition=${message.partition}, offset=${message.offset}")
                meterRegistry.meldingerMottattCounter(message.value.status)
            }
            .filter { message ->
                val publishedTimestamp = message.value.published.fromLocalDateTimeString()
                publishedTimestamp.isAfter(applicationConfig.velgStillingerNyereEnn)
            }
            .onEach { message ->
                logger.trace("Prosesserer melding på topic=${message.topic}, partition=${message.partition}, offset=${message.offset}")
            }
            .forEach { message ->
                runCatching {
                    val stillingRow = message.asStillingRow()
                    lagreStilling(stillingRow)
                    antallLagret++
                }.onFailure { cause ->
                    logger.error("Feil ved mottak av melding", cause)
                }.getOrThrow()
            }
        val slutt = System.currentTimeMillis()
        val millisekunder = slutt - start
        logger.info(
            "Håndterte {} meldinger på {}ms fra topic={}",
            antallMottatt,
            millisekunder,
            applicationConfig.pamStillingerKafkaConsumer.topic
        )
        Span.current().meldingerMottattEvent(antallMottatt, antallLagret, millisekunder)
        meterRegistry.meldingerMottattGauge(antallMottatt, antallLagret)
    }
}