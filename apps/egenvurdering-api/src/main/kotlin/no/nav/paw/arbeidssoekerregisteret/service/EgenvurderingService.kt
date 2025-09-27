package no.nav.paw.arbeidssoekerregisteret.service

import io.ktor.server.plugins.BadRequestException
import io.opentelemetry.api.trace.Span
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingGrunnlag
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingPostgresRepository
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingRepository
import no.nav.paw.arbeidssoekerregisteret.repository.NyesteProfilering
import no.nav.paw.arbeidssoekerregisteret.repository.ProfileringRow
import no.nav.paw.arbeidssoekerregisteret.routes.hentSluttbrukerEllerNull
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.arbeidssoekerregisteret.utils.toProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.kafka.producer.sendDeferred
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.sikkerhetsnivaa
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.Egenvurdering as EgenvurderingDto
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.Profilering as ProfileringDto
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.ProfilertTil as ProfilertTilDto

class EgenvurderingService(
    private val applicationConfig: ApplicationConfig,
    private val kafkaKeysClient: KafkaKeysClient,
    private val producer: Producer<Long, Egenvurdering>,
    private val egenvurderingRepository: EgenvurderingRepository = EgenvurderingPostgresRepository,
) {
    private val logger = buildApplicationLogger

    fun getEgenvurderingGrunnlag(ident: Identitetsnummer): EgenvurderingGrunnlag {
        val nyesteProfilering = egenvurderingRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)

        return when (nyesteProfilering) {
            null -> {
                Span.current().addEvent("ingen_profilering_fra_aapen_periode_uten_egenvurdering")
                EgenvurderingGrunnlag(null)
            }

            else -> {
                Span.current().addEvent("fant_profilering_fra_aapen_periode_uten_egenvurdering")
                EgenvurderingGrunnlag(grunnlag = nyesteProfilering.toProfileringDto())
            }
        }
    }

    suspend fun postEgenvurdering(
        request: EgenvurderingRequest,
        securityContext: SecurityContext,
    ) {
        val sluttbruker = securityContext.hentSluttbrukerEllerNull()
            ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
        val profilering = egenvurderingRepository.finnProfilering(request.profileringId, sluttbruker.ident)
            ?: throw BadRequestException("Fant ingen profilering for oppgit profileringId ${request.profileringId} og ident")

        val kafkaKey = kafkaKeysClient.getIdAndKey(sluttbruker.ident.verdi).key

        val egenvurdering = lagEgenvurdering(
            navProfilering = profilering,
            brukersEgenvurdering = request.egenvurdering,
            metadata = lagMetadata(securityContext)
        )

        val record = ProducerRecord(applicationConfig.producerConfig.egenvurderingTopic, kafkaKey, egenvurdering)

        producer.sendDeferred(record).await()
        egenvurderingRepository.lagreEgenvurdering(egenvurdering)
        logger.info("Egenvurdering med id ${egenvurdering.id} for profilering ${profilering.id} lagret og sendt til kafka")
    }

    private fun lagEgenvurdering(
        navProfilering: ProfileringRow,
        brukersEgenvurdering: EgenvurderingDto,
        metadata: Metadata,
    ) = Egenvurdering(
        UUID.randomUUID(),
        navProfilering.periodeId,
        navProfilering.id,
        metadata,
        navProfilering.profilertTil.toProfilertTil(),
        brukersEgenvurdering.toProfilertTil()
    )

    private fun lagMetadata(securityContext: SecurityContext) = Metadata(
        Instant.now(),
        Bruker(
            BrukerType.SLUTTBRUKER,
            securityContext.hentSluttbrukerEllerNull()!!.ident.verdi,
            securityContext.accessToken.sikkerhetsnivaa()
        ),
        currentRuntimeEnvironment.appNameOrDefaultForLocal(),
        "Bruker har gjort en egenvurdering av profileringsresultatet",
        null
    )
}

private fun NyesteProfilering.toProfileringDto() = ProfileringDto(
    profileringId = id,
    profilertTil = profilertTil.runCatching {
        ProfilertTilDto.valueOf(profilertTil)
    }.getOrElse {
        throw IllegalArgumentException("Ugyldig ApiProfilertTil: $profilertTil")
    }
)

