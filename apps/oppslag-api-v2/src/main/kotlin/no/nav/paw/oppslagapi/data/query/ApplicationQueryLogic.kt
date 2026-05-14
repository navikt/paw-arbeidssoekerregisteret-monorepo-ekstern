package no.nav.paw.oppslagapi.data.query

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.Response
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.oppslagapi.AutorisasjonsTjeneste
import no.nav.paw.oppslagapi.data.finnFolkeregisteridenterKonfliktAware
import no.nav.paw.oppslagapi.model.v2.Tidslinje
import no.nav.paw.security.authentication.model.SecurityContext
import java.util.*

class ApplicationQueryLogic(
    private val autorisasjonsTjeneste: AutorisasjonsTjeneste,
    private val databaseQuerySupport: DatabaseQuerySupport,
    private val kafkaKeysClient: KafkaKeysClient
) {

    @WithSpan(
        value = "hent_tidslinjer_for_identitetsnummer",
        kind = SpanKind.SERVER
    )
    suspend fun hentTidslinjer(
        securityContext: SecurityContext,
        identitetsnummer: Identitetsnummer
    ): Response<List<Tidslinje>> {
        val oenskerTilgangTil = kafkaKeysClient.finnFolkeregisteridenterKonfliktAware(identitetsnummer)
        return autorisasjonsTjeneste.autoriser(
            handling = "Lag tidslinjer for identitetsnummer",
            securityContext = securityContext,
            oenskerTilgangTil = oenskerTilgangTil
        ) {
            val periodeIder = oenskerTilgangTil.flatMap { databaseQuerySupport.hentPerioder(it) }
            val rader = periodeIder.map { it to databaseQuerySupport.hentRaderForPeriode(it) }
            genererTidslinje(rader)
        }
    }

    @WithSpan(
        value = "hent_tidslinjer_for_perioder",
        kind = SpanKind.SERVER
    )
    suspend fun hentTidslinjer(
        securityContext: SecurityContext,
        perioder: List<UUID>
    ): Response<List<Tidslinje>> {
        if (perioder.isEmpty()) {
            return Data(emptyList())
        }
        val rader = perioder.map { it to databaseQuerySupport.hentRaderForPeriode(it) }
        val oenskerTilgangTil = rader
            .flatMap { (_, rader) -> rader.mapNotNull { rad -> rad.identitetsnummer } }
            .distinct()
            .map(::Identitetsnummer)
        return autorisasjonsTjeneste.autoriser(
            handling = "Lag tidslinjer for arbeidssøkerperioder",
            securityContext = securityContext,
            oenskerTilgangTil = oenskerTilgangTil
        ) {
            genererTidslinje(rader)
        }
    }
}
