package no.nav.paw.arbeidssoekerregisteret.service

import io.ktor.server.plugins.BadRequestException
import no.nav.paw.arbeidssoekerregisteret.texas.TexasClient
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingGrunnlag
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.utils.findSisteOpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssoekerregisteret.utils.findSisteProfilering
import no.nav.paw.arbeidssoekerregisteret.utils.isPeriodeAvsluttet
import no.nav.paw.arbeidssoekerregisteret.utils.toApiProfilering
import no.nav.paw.arbeidssoekerregisteret.utils.toProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Egenvurdering
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as RecordMetadata
import no.nav.paw.client.api.oppslag.client.ApiOppslagClient
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.kafka.producer.sendDeferred
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.model.Identitetsnummer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.util.*

class EgenvurderingService(
    private val applicationConfig: ApplicationConfig,
    private val kafkaKeysClient: KafkaKeysClient,
    private val producer: Producer<Long, Egenvurdering>,
    private val texasClient: TexasClient,
    private val oppslagsClient: ApiOppslagClient,
) {
    private val logger = buildApplicationLogger

    suspend fun getEgenvurderingGrunnlag(userToken: String): EgenvurderingGrunnlag {
        val exchangedToken = texasClient.getOnBehalfOfToken(userToken).accessToken
        val arbeidssoekerperioderAggregert = oppslagsClient.findSisteArbeidssoekerperioderAggregert { exchangedToken }
        val sisteProfilering = arbeidssoekerperioderAggregert.findSisteProfilering()
        val innsendtEgenvurdering = sisteProfilering?.egenvurdering
        return if (sisteProfilering == null || innsendtEgenvurdering != null || arbeidssoekerperioderAggregert.isPeriodeAvsluttet()) {
            EgenvurderingGrunnlag(grunnlag = null)
        } else {
            EgenvurderingGrunnlag(
                grunnlag = sisteProfilering.toApiProfilering(),
            )
        }
    }

    suspend fun postEgenvurdering(identitetsnummer: Identitetsnummer, userToken: String, request: EgenvurderingRequest) {
        val exchangedToken = texasClient.getOnBehalfOfToken(userToken).accessToken
        val arbeidssoekerperioderAggregert = oppslagsClient.findSisteArbeidssoekerperioderAggregert { exchangedToken }

        val periode = arbeidssoekerperioderAggregert.firstOrNull()
        val opplysningerOmArbeidssoeker = periode?.findSisteOpplysningerOmArbeidssoeker()
        val profilering = opplysningerOmArbeidssoeker?.profilering
        // TODO: trenger vi sjekke om egenvurdering allerede er sendt?
        if (profilering?.profileringId != request.profileringId) {
            throw BadRequestException("ProfileringId i request (${request.profileringId}) samsvarer ikke med profileringId i siste aggregerte-periode (${profilering?.profileringId})")
        }

        val (_, key) = kafkaKeysClient.getIdAndKey(identitetsnummer.verdi)
        logger.debug("Sender egenvurdering for key $key")
        val metadata = RecordMetadata(
            Instant.now(),
            Bruker(
                BrukerType.SLUTTBRUKER,
                identitetsnummer.verdi,
                "tokenx:Level4"
            ),
            currentRuntimeEnvironment.appNameOrDefaultForLocal(),
            "Bruker har gjort en vurdering av profileringsresultatet",
            null
        )
        val egenvurderingRecord = ProducerRecord(
            applicationConfig.kafkaTopology.egenvurderingTopic,
            key,
            Egenvurdering(
                UUID.randomUUID(),
                periode.periodeId,
                opplysningerOmArbeidssoeker.opplysningerOmArbeidssoekerId,
                profilering.profileringId,
                metadata,
                request.egenvurdering.toProfilertTil()
            )
        )
        producer.sendDeferred(egenvurderingRecord).await().also {
            logger.info("Egenvurdering sendt til Kafka")
        }
    }
}
