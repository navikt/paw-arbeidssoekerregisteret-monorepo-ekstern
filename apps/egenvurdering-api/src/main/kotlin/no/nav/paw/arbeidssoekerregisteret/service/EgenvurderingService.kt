package no.nav.paw.arbeidssoekerregisteret.service

import no.nav.paw.arbeidssoekerregisteret.texas.TexasClient
import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingGrunnlag
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.EgenvurderingRequest
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.Egenvurdering as ApiEgenvurdering
import no.nav.paw.arbeidssoekerregisteret.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Egenvurdering
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as RecordMetadata
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.client.api.oppslag.client.ApiOppslagClient
import no.nav.paw.client.api.oppslag.models.ArbeidssoekerperiodeAggregertResponse
import no.nav.paw.client.api.oppslag.models.ProfileringAggregertResponse
import no.nav.paw.client.api.oppslag.models.ProfileringsResultat
import no.nav.paw.kafka.producer.sendDeferred
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.model.Identitetsnummer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.Profilering as ApiProfilering
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.api.models.ProfilertTil as ApiProfilertTil

class EgenvurderingService(
    private val applicationConfig: ApplicationConfig,
    private val kafkaKeysClient: KafkaKeysClient,
    private val producer: Producer<Long, Egenvurdering>,
    private val texasClient: TexasClient,
    private val oppslagsClient: ApiOppslagClient,
) {
    private val logger = buildApplicationLogger

    @Suppress("KotlinUnreachableCode", "Midlertidig død kode i metoden")
    suspend fun getEgenvurderingGrunnlag(userToken: String): EgenvurderingGrunnlag {
        val exchangedToken = texasClient.getOnBehalfOfToken(userToken).accessToken
        return EgenvurderingGrunnlag(grunnlag = null)

        val arbeidssoekerperioderAggregert = oppslagsClient.findSisteArbeidssoekerperioderAggregert { exchangedToken }
        val profilering = arbeidssoekerperioderAggregert.findSisteProfilering()
        val egenvurdering = profilering?.egenvurdering
        return if (profilering == null || egenvurdering != null || arbeidssoekerperioderAggregert.isPeriodeAvsluttet()) {
            EgenvurderingGrunnlag(grunnlag = null)
        } else {
            EgenvurderingGrunnlag(
                grunnlag = profilering.toApiProfilering(),
            )
        }
    }

    suspend fun postEgenvurdering(identitetsnummer: Identitetsnummer, userToken: String, request: EgenvurderingRequest) {
        val exchangedToken = texasClient.getOnBehalfOfToken(userToken).accessToken
        val arbeidssoekerperioderAggregert = oppslagsClient.findSisteArbeidssoekerperioderAggregert { exchangedToken }

        val periode = arbeidssoekerperioderAggregert.firstOrNull()
        val opplysningerOmArbeidssoeker = periode?.opplysningerOmArbeidssoeker?.maxByOrNull { it.sendtInnAv.tidspunkt }
        val profilering = opplysningerOmArbeidssoeker?.profilering
        if (profilering?.profileringId != request.profileringId) {
            throw IllegalArgumentException("ProfileringId i request (${request.profileringId}) samsvarer ikke med profileringId i siste aggregerte-periode (${profilering?.profileringId})")
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
            "paw-arbeidssoekerregisteret-egenvurdering-api",
            "bruker sendte inn egenvurdering",
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

fun List<ArbeidssoekerperiodeAggregertResponse>.findSisteProfilering(): ProfileringAggregertResponse? =
    this.firstOrNull()?.opplysningerOmArbeidssoeker?.maxByOrNull { it.sendtInnAv.tidspunkt }?.profilering

fun List<ArbeidssoekerperiodeAggregertResponse>.isPeriodeAvsluttet(): Boolean =
    this.isNotEmpty() && this[0].avsluttet != null

fun ApiEgenvurdering.toProfilertTil(): ProfilertTil =
    when (this) {
        ApiEgenvurdering.ANTATT_GODE_MULIGHETER -> ProfilertTil.ANTATT_GODE_MULIGHETER
        ApiEgenvurdering.ANTATT_BEHOV_FOR_VEILEDNING -> ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        ApiEgenvurdering.OPPGITT_HINDRINGER -> ProfilertTil.OPPGITT_HINDRINGER
    }

fun ProfileringAggregertResponse.toApiProfilering(): ApiProfilering =
    ApiProfilering(
        profileringId = profileringId,
        profilertTil = profilertTil.toApiProfilertTil() ?: throw IllegalArgumentException("Ugyldig profilertTil: $profilertTil"),
    )

fun ProfileringsResultat.toApiProfilertTil(): ApiProfilertTil? {
    return when (this) {
        ProfileringsResultat.ANTATT_GODE_MULIGHETER -> ApiProfilertTil.ANTATT_GODE_MULIGHETER
        ProfileringsResultat.ANTATT_BEHOV_FOR_VEILEDNING -> ApiProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        ProfileringsResultat.OPPGITT_HINDRINGER -> ApiProfilertTil.OPPGITT_HINDRINGER
        else -> null
    }
}
