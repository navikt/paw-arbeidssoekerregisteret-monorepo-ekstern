package no.nav.paw.ledigestillinger.service

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.api.models.Fylke
import no.nav.paw.ledigestillinger.api.models.Kategori
import no.nav.paw.ledigestillinger.api.models.Paging
import no.nav.paw.ledigestillinger.api.models.Stilling
import no.nav.paw.ledigestillinger.config.ApplicationConfig
import no.nav.paw.ledigestillinger.exception.StillingIkkeFunnetException
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.asStillingRow
import no.nav.paw.ledigestillinger.model.dao.ArbeidsgivereTable
import no.nav.paw.ledigestillinger.model.dao.BeliggenheterTable
import no.nav.paw.ledigestillinger.model.dao.EgenskaperTable
import no.nav.paw.ledigestillinger.model.dao.KategorierTable
import no.nav.paw.ledigestillinger.model.dao.KlassifiseringerTable
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.model.dao.insert
import no.nav.paw.ledigestillinger.model.dao.selectIdByUUID
import no.nav.paw.ledigestillinger.model.dao.selectRowByUUID
import no.nav.paw.ledigestillinger.model.dao.selectRowsByKategorierAndFylker
import no.nav.paw.ledigestillinger.model.dao.updateById
import no.nav.paw.ledigestillinger.util.fromLocalDateTimeString
import no.nav.paw.ledigestillinger.util.meldingerMottattCounter
import no.nav.paw.ledigestillinger.util.meldingerMottattEvent
import no.nav.paw.ledigestillinger.util.meldingerMottattGauge
import no.nav.paw.logging.logger.buildLogger
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

    fun hentStilling(uuid: UUID): Stilling = transaction {
        StillingerTable.selectRowByUUID(uuid)?.asDto() ?: throw StillingIkkeFunnetException()
    }

    fun finnStillinger(
        soekeord: Collection<String>,
        kategorier: Collection<Kategori>,
        fylker: Collection<Fylke>,
        paging: Paging = Paging()
    ): List<Stilling> = transaction {
        StillingerTable.selectRowsByKategorierAndFylker(
            soekeord = soekeord,
            kategorier = kategorier.map { it.kode },
            fylker = fylker.mapNotNull { it.fylkesnummer },
            paging = paging
        ).map { it.asDto() }
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
                    antallLagret++
                    handleMessage(message)
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

    @WithSpan
    fun handleMessage(message: Message<UUID, Ad>) {
        val uuid = message.key
        val stillingRow = message.asStillingRow()
        val existingId = StillingerTable.selectIdByUUID(uuid)
        if (existingId == null) {
            val id = StillingerTable.insert(stillingRow)
            stillingRow.arbeidsgiver?.let { row ->
                ArbeidsgivereTable.insert(
                    parentId = id,
                    row = row
                )
            }
            stillingRow.kategorier.forEach { row ->
                KategorierTable.insert(
                    parentId = id,
                    row = row
                )
            }
            stillingRow.klassifiseringer.forEach { row ->
                KlassifiseringerTable.insert(
                    parentId = id,
                    row = row
                )
            }
            stillingRow.beliggenheter.forEach { row ->
                BeliggenheterTable.insert(
                    parentId = id,
                    row = row
                )
            }
            stillingRow.egenskaper.forEach { row ->
                EgenskaperTable.insert(
                    parentId = id,
                    row = row
                )
            }
        } else {
            StillingerTable.updateById(
                id = existingId,
                row = stillingRow
            )
        }
    }
}