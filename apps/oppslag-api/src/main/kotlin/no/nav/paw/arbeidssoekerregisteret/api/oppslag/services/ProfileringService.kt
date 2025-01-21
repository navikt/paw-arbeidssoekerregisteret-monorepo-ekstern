package no.nav.paw.arbeidssoekerregisteret.api.oppslag.services

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Paging
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories.ProfileringRepository
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.profileringerKafkaCounter
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.profileringerKafkaTrace
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.model.Identitetsnummer
import org.apache.kafka.clients.consumer.ConsumerRecords
import java.util.*

class ProfileringService(
    private val meterRegistry: MeterRegistry,
    private val profileringRepository: ProfileringRepository
) {
    private val logger = buildLogger

    fun finnProfileringerForPeriodeIdList(
        periodeIdList: Collection<UUID>,
        paging: Paging = Paging()
    ): List<ProfileringResponse> =
        profileringRepository.finnProfileringerForPeriodeIdList(periodeIdList, paging)
            .map { it.toProfileringResponse() }

    fun finnProfileringerForIdentiteter(
        identitetsnummerList: Collection<Identitetsnummer>,
        paging: Paging = Paging()
    ): List<ProfileringResponse> =
        profileringRepository.finnProfileringerForIdentiteter(identitetsnummerList, paging)
            .map { it.toProfileringResponse() }

    fun lagreAlleProfileringer(profileringer: Iterable<Profilering>) =
        profileringRepository.lagreAlleProfileringer(profileringer)

    @WithSpan("paw.kafka.consumer")
    fun handleRecords(records: ConsumerRecords<Long, Profilering>) {
        Span.current().profileringerKafkaTrace()

        if (!records.isEmpty) {
            logger.info("Mottok {} profileringer fra Kafka", records.count())
            meterRegistry.profileringerKafkaCounter(records.count())
            lagreAlleProfileringer(records.map { it.value() })
        }
    }
}
