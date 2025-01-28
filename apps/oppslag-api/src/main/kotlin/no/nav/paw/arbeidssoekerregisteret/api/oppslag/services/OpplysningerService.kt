package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toOpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.OpplysningerRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.opplysningerKafkaCounter
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.opplysningerKafkaTrace
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.model.Identitetsnummer
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.util.*

class OpplysningerService(
    private val meterRegistry: MeterRegistry,
    private val opplysningerRepository: OpplysningerRepository
) {
    private val logger = buildLogger

    fun finnOpplysningerForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<OpplysningerOmArbeidssoekerResponse> =
        opplysningerRepository.finnOpplysningerForPeriodeIdList(periodeIdList, paging)
            .map { it.toOpplysningerOmArbeidssoekerResponse() }

    fun finnOpplysningerForIdentiteter(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<OpplysningerOmArbeidssoekerResponse> =
        opplysningerRepository.finnOpplysningerForIdentiteter(identitetsnummerList, paging)
            .map { it.toOpplysningerOmArbeidssoekerResponse() }

    fun lagreOpplysninger(opplysninger: OpplysningerOmArbeidssoeker) =
        opplysningerRepository.lagreOpplysninger(opplysninger)

    @WithSpan("paw.kafka.consumer")
    fun handleRecords(records: ConsumerRecords<Long, OpplysningerOmArbeidssoeker>) {
        Span.current().opplysningerKafkaTrace(records.count())
        logger.info("Mottok {} opplysninger fra Kafka", records.count())
        meterRegistry.opplysningerKafkaCounter(records.count())
        opplysningerRepository.lagreOpplysninger(records.map { it.value() })
    }
}
