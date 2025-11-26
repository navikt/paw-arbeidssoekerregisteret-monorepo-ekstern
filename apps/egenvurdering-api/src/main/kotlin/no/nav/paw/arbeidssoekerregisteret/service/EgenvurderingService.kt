package no.nav.paw.arbeidssoekerregisteret.service

import io.ktor.server.plugins.BadRequestException
import io.opentelemetry.api.trace.Span
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.mapping.asAvro
import no.nav.paw.arbeidssoekerregisteret.mapping.asDto
import no.nav.paw.arbeidssoekerregisteret.mapping.erGyldig
import no.nav.paw.arbeidssoekerregisteret.mapping.validerEgenvurdering
import no.nav.paw.arbeidssoekerregisteret.model.EgenvurderingGrunnlag
import no.nav.paw.arbeidssoekerregisteret.model.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.model.EgenvurdertTil
import no.nav.paw.arbeidssoekerregisteret.model.ProfileringRow
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingPostgresRepository
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.config.env.appNameOrDefaultForLocal
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.model.IdentitetType
import no.nav.paw.logging.logger.buildLogger
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.util.*

class EgenvurderingService(
    private val applicationConfig: ApplicationConfig,
    private val kafkaKeysClient: KafkaKeysClient,
    private val egenvurderingKafkaProducer: Producer<Long, Egenvurdering>,
    private val egenvurderingRepository: EgenvurderingRepository = EgenvurderingPostgresRepository,
) {
    private val logger = buildLogger

    fun getEgenvurderingGrunnlag(identitetsnummer: Identitetsnummer): EgenvurderingGrunnlag {
        egenvurderingRepository.finnNyesteProfilering(identitetsnummer)
            ?.let { nyesteProfilering ->
                if (nyesteProfilering.erGyldig(applicationConfig.prodsettingstidspunktEgenvurdering)) {
                    logger.info(
                        "Fant gyldig profilering, prodsettingstidspunkt: {}",
                        applicationConfig.prodsettingstidspunktEgenvurdering
                    )
                    Span.current().addEvent("fant_profilering_fra_aapen_periode_uten_egenvurdering")
                    return EgenvurderingGrunnlag(grunnlag = nyesteProfilering.asDto())
                }
            }

        hentAlternativeIdenter(identitetsnummer).forEach { identitetsnummer ->
            val nyesteProfilering = egenvurderingRepository.finnNyesteProfilering(identitetsnummer)
            if (nyesteProfilering != null && nyesteProfilering.erGyldig(applicationConfig.prodsettingstidspunktEgenvurdering)) {
                logger.info(
                    "Fant gyldig profilering for alternativ identitetsnummer, prodsettingstidspunkt: {}",
                    applicationConfig.prodsettingstidspunktEgenvurdering
                )
                Span.current().addEvent("fant_profilering_fra_aapen_periode_uten_egenvurdering")
                return EgenvurderingGrunnlag(grunnlag = nyesteProfilering.asDto())
            }
        }

        logger.info(
            "Fant ingen gyldig profilering, prodsettingstidspunkt: {}",
            applicationConfig.prodsettingstidspunktEgenvurdering
        )
        Span.current().addEvent("ingen_profilering_fra_aapen_periode_uten_egenvurdering")
        return EgenvurderingGrunnlag(null)
    }

    suspend fun publiserOgLagreEgenvurdering(
        request: EgenvurderingRequest,
        identitetsnummer: Identitetsnummer,
        sikkerhetsnivaa: String
    ) {
        val profilering = egenvurderingRepository.finnProfilering(request.profileringId, identitetsnummer)
            ?: throw BadRequestException("Fant ingen profilering for oppgit profileringId ${request.profileringId} og ident")

        val egenvurdering = lagEgenvurdering(
            navProfilering = profilering,
            brukersEgenvurdering = request.egenvurdering,
            metadata = lagMetadata(identitetsnummer, sikkerhetsnivaa)
        ).validerEgenvurdering()

        val kafkaKey = kafkaKeysClient.getIdAndKey(identitetsnummer.value).key
        val record = ProducerRecord(applicationConfig.producerConfig.egenvurderingTopic, kafkaKey, egenvurdering)

        transaction {
            egenvurderingRepository.lagreEgenvurdering(egenvurdering)
            egenvurderingKafkaProducer.send(record).get()
        }
        logger.info("Egenvurdering med id ${egenvurdering.id} for profilering ${profilering.id} lagret og sendt til kafka")
    }

    private fun hentAlternativeIdenter(identitetsnummer: Identitetsnummer): List<Identitetsnummer> = runBlocking {
        runCatching {
            val identiteter = kafkaKeysClient.getIdentiteter(identitetsnummer.value)
            identiteter.identiteter.filter { ident ->
                ident.type == IdentitetType.FOLKEREGISTERIDENT && ident.identitet != identitetsnummer.value
            }.distinct().map { Identitetsnummer(value = it.identitet) }
        }.onFailure { throwable ->
            logger.warn("Kall mot kafkaKey /identiteter feilet", throwable)
        }.getOrDefault(emptyList())
    }

    private fun lagEgenvurdering(
        navProfilering: ProfileringRow,
        brukersEgenvurdering: EgenvurdertTil,
        metadata: Metadata,
    ) = Egenvurdering(
        UUID.randomUUID(),
        navProfilering.periodeId,
        navProfilering.id,
        metadata,
        navProfilering.profilertTil.asAvro(),
        brukersEgenvurdering.asAvro()
    )

    private fun lagMetadata(
        identitetsnummer: Identitetsnummer,
        sikkerhetsnivaa: String
    ): Metadata = Metadata(
        Instant.now(),
        Bruker(
            BrukerType.SLUTTBRUKER,
            identitetsnummer.value,
            sikkerhetsnivaa
        ),
        currentRuntimeEnvironment.appNameOrDefaultForLocal(),
        "Bruker har gjort en egenvurdering av profileringsresultatet",
        null
    )
}