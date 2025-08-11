package no.nav.paw.arbeidssoekerregisteret.service

import io.ktor.server.plugins.BadRequestException
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
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import no.nav.paw.client.api.oppslag.client.ApiOppslagClient
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.kafka.producer.sendDeferred
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.security.authentication.model.ACR
import no.nav.paw.security.authentication.token.AccessToken
import no.nav.paw.security.texas.TexasClient
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as RecordMetadata

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
        val innsendtEgenvurderinger = sisteProfilering?.egenvurderinger ?: emptyList()
        return if (sisteProfilering == null || innsendtEgenvurderinger.isNotEmpty() || arbeidssoekerperioderAggregert.isPeriodeAvsluttet()) {
            EgenvurderingGrunnlag(grunnlag = null)
        } else {
            EgenvurderingGrunnlag(
                grunnlag = sisteProfilering.toApiProfilering(),
            )
        }
    }

    suspend fun postEgenvurdering(
        identitetsnummer: Identitetsnummer,
        request: EgenvurderingRequest,
        accessToken: AccessToken,
    ) {
        val exchangedToken = texasClient.getOnBehalfOfToken(accessToken.jwt).accessToken
        val arbeidssoekerperioderAggregert = oppslagsClient.findSisteArbeidssoekerperioderAggregert { exchangedToken }

        val periode = arbeidssoekerperioderAggregert.firstOrNull()
        val opplysningerOmArbeidssoeker = periode?.findSisteOpplysningerOmArbeidssoeker()
        val profilering = opplysningerOmArbeidssoeker?.profilering
        val egenvurderinger = profilering?.egenvurderinger ?: emptyList()

        if (profilering?.profileringId != request.profileringId) {
            throw BadRequestException("ProfileringId i request (${request.profileringId}) samsvarer ikke med profileringId i siste aggregerte-periode (${profilering?.profileringId})")
        }

        if (egenvurderinger.isNotEmpty()) {
            throw BadRequestException("Egenvurdering er allerede sendt for denne profileringen (${profilering.profileringId})")
        }

        val (_, key) = kafkaKeysClient.getIdAndKey(identitetsnummer.verdi)
        logger.debug("Sender egenvurdering for key $key")
        val metadata = RecordMetadata(
            Instant.now(),
            Bruker(
                BrukerType.SLUTTBRUKER,
                identitetsnummer.verdi,
                accessToken.claims.getOrThrow(ACR)
            ),
            currentRuntimeEnvironment.appNameOrDefaultForLocal(),
            "Bruker har gjort en egenvurdering av profileringsresultatet",
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
                profilering.profilertTil.toProfilertTil(),
                request.egenvurdering.toProfilertTil()
            )
        )
        producer.sendDeferred(egenvurderingRecord).await().also {
            logger.info("Egenvurdering sendt til Kafka")
        }
    }
}
