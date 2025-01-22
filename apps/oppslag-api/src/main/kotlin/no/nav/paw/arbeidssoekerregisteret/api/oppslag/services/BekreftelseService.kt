package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BekreftelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toBekreftelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.BekreftelseRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.bekreftelserKafkaCounter
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.bekreftelserKafkaTrace
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.model.Identitetsnummer
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.util.*

class BekreftelseService(
    private val meterRegistry: MeterRegistry,
    private val bekreftelseRepository: BekreftelseRepository
) {
    private val logger = buildLogger

    fun finnBekreftelserForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<BekreftelseResponse> =
        bekreftelseRepository.finnBekreftelserForPeriodeIdList(periodeIdList, paging)
            .map { it.toBekreftelseResponse() }

    fun finnBekreftelserForIdentitetsnummerList(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<BekreftelseResponse> =
        bekreftelseRepository.finnBekreftelserForIdentitetsnummerList(identitetsnummerList, paging)
            .map { it.toBekreftelseResponse() }

    fun lagreBekreftelse(bekreftelse: Bekreftelse) =
        bekreftelseRepository.lagreBekreftelse(bekreftelse)

    @WithSpan("paw.kafka.consumer")
    fun handleRecords(records: ConsumerRecords<Long, Bekreftelse>) {
        Span.current().bekreftelserKafkaTrace(records.count())
        logger.info("Mottok {} bekreftelser fra Kafka", records.count())
        meterRegistry.bekreftelserKafkaCounter(records.count())
        bekreftelseRepository.lagreBekreftelser(records.map { it.value() })
    }
}