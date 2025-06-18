package no.nav.paw.arbeidssoekerregisteret.service

import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingGrunnlag
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.Egenvurdering as ApiEgenvurdering
import no.nav.paw.arbeidssoekerregisteret.utils.EgenvurderingSerializer
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Egenvurdering
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as RecordMetadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafka.producer.sendDeferred
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.model.Identitetsnummer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.Profilering as ApiProfilering
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.ProfilertTil as ApiProfilertTil

class EgenvurderingService(
    private val applicationConfig: ApplicationConfig,
    private val kafkaConfig: KafkaConfig,
    private val kafkaKeysClient: KafkaKeysClient,
    private val profileringState: ReadOnlyKeyValueStore<Long, Profilering>
) {
    private val logger = buildApplicationLogger
    private var producer: Producer<Long, Egenvurdering>

    init {
        val kafkaFactory = KafkaFactory(kafkaConfig)
        producer = kafkaFactory.createProducer(
            clientId = "${kafkaConfig.applicationIdPrefix}_${applicationConfig.kafkaTopology.producerVersion}",
            keySerializer = LongSerializer::class,
            valueSerializer = EgenvurderingSerializer::class
        )
    }

    suspend fun getEgenvurderingGrunnlag(identitetsnummer: Identitetsnummer): EgenvurderingGrunnlag? {
        val (arbeidssoekerId, _) = kafkaKeysClient.getIdAndKey(identitetsnummer.verdi)
        val grunnlag = profileringState.get(arbeidssoekerId)
        return EgenvurderingGrunnlag(
            grunnlag = grunnlag.toApiProfilering(),
        )
    }

    suspend fun postEgenvurdering(identitetsnummer: Identitetsnummer, request: EgenvurderingRequest) {
        val (arbeidssoekerId, key) = kafkaKeysClient.getIdAndKey(identitetsnummer.verdi)
        logger.info("Sender egenvurdering for key $key")
        val metadata = RecordMetadata(
            Instant.now(),
            Bruker(
                BrukerType.SLUTTBRUKER,
                identitetsnummer.verdi,
                "tokenx:Level4"
            ),
            "todo",
            "todo",
            null
        ) // TODO: fiks verdier for metadata
        val record = ProducerRecord(
            applicationConfig.kafkaTopology.egenvurderingTopic,
            key,
            Egenvurdering(
                UUID.randomUUID(),
                request.periodeId,
                request.opplysningerOmArbeidssoekerId,
                request.profileringId,
                metadata,
                request.egenvurdering.toProfilertTil()
            )
        )
        producer.sendDeferred(record).await().also {
            logger.info("Egenvurdering sendt til Kafka")
        }
    }
}

fun ApiEgenvurdering.toProfilertTil(): ProfilertTil =
    when (this) {
        ApiEgenvurdering.ANTATT_GODE_MULIGHETER -> ProfilertTil.ANTATT_GODE_MULIGHETER
        ApiEgenvurdering.ANTATT_BEHOV_FOR_VEILEDNING -> ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        ApiEgenvurdering.OPPGITT_HINDRINGER -> ProfilertTil.OPPGITT_HINDRINGER
    }

fun Profilering.toApiProfilering(): ApiProfilering =
    ApiProfilering(
        profileringId = id,
        profilertTil = profilertTil.toApiProfilertTil() ?: throw IllegalArgumentException("Ugyldig profilertTil: $profilertTil"),
    )

fun ProfilertTil.toApiProfilertTil(): ApiProfilertTil? {
    return when (this) {
        ProfilertTil.ANTATT_GODE_MULIGHETER -> ApiProfilertTil.ANTATT_GODE_MULIGHETER
        ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ApiProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        ProfilertTil.OPPGITT_HINDRINGER -> ApiProfilertTil.OPPGITT_HINDRINGER
        else -> null
    }
}
