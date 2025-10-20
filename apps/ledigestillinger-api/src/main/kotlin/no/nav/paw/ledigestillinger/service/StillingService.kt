package no.nav.paw.ledigestillinger.service

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.hwm.Message
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
import no.nav.paw.ledigestillinger.model.dao.updateById
import no.nav.paw.ledigestillinger.util.fromIsoString
import no.nav.paw.ledigestillinger.util.meldingerMottattCounter
import no.nav.paw.ledigestillinger.util.meldingerMottattEvent
import no.nav.paw.ledigestillinger.util.meldingerMottattGauge
import no.nav.paw.logging.logger.buildLogger
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

class StillingService(
    private val applicationConfig: ApplicationConfig,
    private val meterRegistry: PrometheusMeterRegistry
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

    @WithSpan
    fun handleMessages(messages: Sequence<Message<UUID, Ad>>): Unit = transaction {
        val antallMottatt = messages.count()
        var antallLagret = 0
        messages
            .onEach { message -> meterRegistry.meldingerMottattCounter(message.value.status) }
            .filter { message ->
                message.value.published.fromIsoString().isAfter(applicationConfig.velgStillingerNyereEnn)
            }
            .onEach { message -> logger.debug("Mottatt melding på topic=${message.topic}, partition=${message.partition}, offset=${message.offset}") }
            .forEach { message ->
                runCatching {
                    antallLagret++
                    handleMessage(message)
                }.onFailure { cause ->
                    logger.error("Feil ved mottak av melding", cause)
                }.getOrThrow()
            }
        Span.current().meldingerMottattEvent(antallMottatt, antallLagret)
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