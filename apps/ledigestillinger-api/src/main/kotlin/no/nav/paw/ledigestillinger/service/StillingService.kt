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
import no.nav.paw.ledigestillinger.model.dao.insert
import no.nav.paw.ledigestillinger.model.dao.selectIdByUUID
import no.nav.paw.ledigestillinger.model.dao.selectRowByUUID
import no.nav.paw.ledigestillinger.model.dao.selectRowsByKategorierAndFylker
import no.nav.paw.ledigestillinger.model.dao.selectRowsByUUIDList
import no.nav.paw.ledigestillinger.model.dao.updateById
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
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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
        StillingerTable.selectRowByUUID(uuid)?.asDto() ?: throw StillingIkkeFunnetException()
    }

    @WithSpan
    fun finnStillingerByUuidListe(
        uuidListe: Collection<UUID>
    ): List<Stilling> = transaction {
        Span.current().finnStillingerByUuidListeEvent(antallUuider = uuidListe.size)
        meterRegistry.finnStillingerByUuidListeGauge(antallUuider = uuidListe.size)
        StillingerTable.selectRowsByUUIDList(uuidListe).map { it.asDto() }
    }

    @WithSpan
    fun finnStillingerByEgenskaper(
        soekeord: Collection<String>,
        kategorier: Collection<String>,
        fylker: Collection<Fylke>,
        paging: Paging = Paging()
    ): List<Stilling> = transaction {
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
        StillingerTable.selectRowsByKategorierAndFylker(
            soekeord = soekeord,
            kategorier = kategorier,
            fylker = fylker,
            paging = paging
        ).map { it.asDto() }
    }

    @WithSpan
    fun lagreStilling(stillingRow: StillingRow) {
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