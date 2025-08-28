package no.nav.paw.oppslagapi.data.query

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ApiV2BekreftelserPostRequest
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelserResponse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Tidslinje
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.Response
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.AutorisasjonsTjeneste
import no.nav.paw.security.authentication.model.SecurityContext
import java.time.Instant
import java.util.UUID

class ApplicationQueryLogic(
    private val autorisasjonsTjeneste: AutorisasjonsTjeneste,
    private val databaseQuerySupport: DatabaseQeurySupport,
    private val kafkaKeysClient: KafkaKeysClient
) {

    @WithSpan(
        value = "hent_tidslinjer",
        kind = SpanKind.SERVER
    )
    suspend fun hentTidslinjer(
        securityContext: SecurityContext,
        identitetsnummer: Identitetsnummer
    ): Response<List<Tidslinje>> {
        val identieteter = kafkaKeysClient.getInfo(identitetsnummer.verdi)
            ?.info
            ?.pdlData
            ?.id
            ?.filter { it.gruppe.equals("FOLKEREGISTERIDENT", ignoreCase = true)  }
            ?.map{ it.id }
            ?.map(::Identitetsnummer)
            ?: listOf(identitetsnummer)

        return autorisasjonsTjeneste.autoriser(
            handling = "Hent arbeidssøker data",
            securityContext = securityContext,
            oenskerTilgangTil = identieteter
        ) {
            val periodeIder = identieteter.flatMap { id -> databaseQuerySupport.hentPerioder(id ) }
            val perioder = periodeIder.map { it to databaseQuerySupport.hentRaderForPeriode(it) }
            genererTidslinje(perioder)
        }
    }

    @WithSpan(
        value = "hent_bekreftelser",
        kind = SpanKind.SERVER
    )
    suspend fun hentBekreftelser(
        securityContext: SecurityContext,
        request: ApiV2BekreftelserPostRequest
    ): Response<BekreftelserResponse> {
        if (request.perioder.isEmpty()) {
            return Data(BekreftelserResponse(emptyList()))
        }
        val tidslinjer = request.perioder.mapNotNull { periodeId ->
            val rader = databaseQuerySupport.hentRaderForPeriode(periodeId)
            genererTidslinje(periodeId, rader)
        }
        val identerOensketInfoOm = tidslinjer.map { it.identitetsnummer }.toSet()
        return autorisasjonsTjeneste.autoriser(
            handling = "Hent bekreftelser for arbeidssøkerperiode",
            securityContext = securityContext,
            oenskerTilgangTil = identerOensketInfoOm.map(::Identitetsnummer)
        ) {
            BekreftelserResponse(
                bekreftelser = tidslinjer
                    .flatMap { it.bekreftelser }
                    .sortedByDescending { it.bekreftelse?.svar?.sendtInnAv?.tidspunkt ?: Instant.EPOCH }
            )
        }
    }

    @WithSpan(
        value = "lag_tidslinjer",
        kind = SpanKind.SERVER
    )
    suspend fun lagTidslinjer(
        securityContext: SecurityContext,
        perioder: List<UUID>
    ): Response<List<Tidslinje>> {
        if (perioder.isEmpty()) {
            return Data(emptyList())
        }
        val rader = perioder.map { periodeId ->
            periodeId to databaseQuerySupport.hentRaderForPeriode(periodeId)
        }
        return autorisasjonsTjeneste.autoriser(
            handling = "Lag tidslinjer for arbeidssøkerperioder",
            securityContext = securityContext,
            oenskerTilgangTil = rader
                .flatMap { (_, rader) -> rader.mapNotNull { rad -> rad.identitetsnummer } }
                .distinct()
                .map(::Identitetsnummer)
        ) {
            genererTidslinje(rader)
        }
    }
}
