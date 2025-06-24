package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.EgenvurderingResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toEgenvurderingResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.EgenvurderingRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.egenvurderingKafkaTrace
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.egenvurderingerKafkaCounter
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.traceparent
import no.nav.paw.arbeidssokerregisteret.api.v1.Egenvurdering
import no.nav.paw.model.Identitetsnummer
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.util.*

class EgenvurderingService(
    private val meterRegistry: MeterRegistry,
    private val egenvurderingRepository: EgenvurderingRepository
) {
    private val logger = buildLogger

    fun finnEgenvurderingerForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<EgenvurderingResponse> =
        egenvurderingRepository.finnEgenvurderingerForPeriodeIdList(periodeIdList, paging)
            .map { it.toEgenvurderingResponse() }

    fun finnEgenvurderingerForIdentiteter(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<EgenvurderingResponse> =
        egenvurderingRepository.finnEgenvurderingerForIdentiteter(identitetsnummerList, paging)
            .map { it.toEgenvurderingResponse() }

    fun lagreEgenvurdering(egenvurdering: Egenvurdering) =
        egenvurderingRepository.lagreEgenvurdering(egenvurdering)

    @WithSpan("paw.kafka.consumer")
    fun handleRecords(records: ConsumerRecords<Long, Egenvurdering>) {
        Span.current().egenvurderingKafkaTrace(records.count())
        logger.info("Mottok {} egnvurderinger fra Kafka", records.count())
        meterRegistry.egenvurderingerKafkaCounter(records.count())
        egenvurderingRepository.lagreEgenvurderinger(records.map { it.traceparent() to it.value() })
    }
}
