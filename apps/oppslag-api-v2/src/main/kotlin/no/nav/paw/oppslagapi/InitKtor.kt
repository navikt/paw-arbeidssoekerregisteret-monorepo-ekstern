package no.nav.paw.oppslagapi

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ApiV2BekreftelserPostRequest
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelseMedMetadata
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BekreftelserResponse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bekreftelsesloesning
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bruker
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Metadata
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Svar
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.oppslagapi.health.HasStarted
import no.nav.paw.oppslagapi.health.IsAlive
import no.nav.paw.oppslagapi.health.IsReady
import no.nav.paw.oppslagapi.health.internalRoutes
import no.nav.paw.security.authentication.config.AuthProvider
import no.nav.paw.security.authentication.model.Anonym
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.plugin.installAuthenticationPlugin
import no.nav.paw.tilgangskontroll.client.TilgangsTjenesteForAnsatte
import java.time.Duration
import java.time.Instant
import java.util.*

fun <A> initKtor(
    prometheusRegistry: PrometheusMeterRegistry,
    meterBinders: List<MeterBinder>,
    healthIndicator: A,
    authProviders: List<AuthProvider>,
    openApiSpecFile: String = "openapi/openapi-spec.yaml",
    kafkaKeysClient: KafkaKeysClient
): EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration> where
        A : IsAlive, A : IsReady, A : HasStarted {

    return embeddedServer(Netty, port = 8080) {
        install(MicrometerMetrics) {
            registry = prometheusRegistry
            this.meterBinders = meterBinders
        }
        installAuthenticationPlugin(authProviders)
        routing {
            internalRoutes(healthIndicator, prometheusRegistry)
            swaggerUI(path = "documentation/openapi-spec", swaggerFile = openApiSpecFile)
            route("/api/v2/bekreftelser") {
                post<ApiV2BekreftelserPostRequest> {
                    val request = call.receive<ApiV2BekreftelserPostRequest>()
                    if (request.perioder.isEmpty()) {
                        call.respond(HttpStatusCode.OK, BekreftelserResponse(emptyList()))
                        return@post
                    }
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = BekreftelserResponse( listOf(
                            BekreftelseMedMetadata(
                                status = BekreftelseMedMetadata.Status.UTENFOR_PERIODE,
                                bekreftelse = Bekreftelse(
                                    periodeId = request.perioder.first(),
                                    bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER,
                                    id = UUID.randomUUID(),
                                    svar = Svar(
                                        sendtInnAv = Metadata(
                                            tidspunkt = Instant.now() - Duration.ofDays(1),
                                            utfoertAv = Bruker(
                                                type = Bruker.Type.SLUTTBRUKER,
                                                id = "12345678901",
                                                sikkerhetsnivaa = "idporten-loa-high"
                                            ),
                                            kilde = "dp",
                                            aarsak = "levert",
                                            tidspunktFraKilde = null
                                        ),
                                        gjelderFra = Instant.now() - Duration.ofDays(10),
                                        gjelderTil = Instant.now() - Duration.ofDays(5),
                                        harJobbetIDennePerioden = true,
                                        vilFortsetteSomArbeidssoeker = true
                                    )
                                )
                            ),
                            BekreftelseMedMetadata(
                                status = BekreftelseMedMetadata.Status.GYLDIG,
                                bekreftelse = Bekreftelse(
                                    periodeId = request.perioder.first(),
                                    bekreftelsesloesning = Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET,
                                    id = UUID.randomUUID(),
                                    svar = Svar(
                                        sendtInnAv = Metadata(
                                            tidspunkt = Instant.now() - Duration.ofDays(1),
                                            utfoertAv = Bruker(
                                                type = Bruker.Type.SLUTTBRUKER,
                                                id = "12345678901",
                                                sikkerhetsnivaa = "idporten-loa-high"
                                            ),
                                            kilde = "bekreftelse",
                                            aarsak = "levert",
                                            tidspunktFraKilde = null
                                        ),
                                        gjelderFra = Instant.now() - Duration.ofDays(10),
                                        gjelderTil = Instant.now() - Duration.ofDays(5),
                                        harJobbetIDennePerioden = true,
                                        vilFortsetteSomArbeidssoeker = true
                                    )
                                )
                            ),
                            BekreftelseMedMetadata(
                                status = BekreftelseMedMetadata.Status.UVENTET_KILDE,
                                bekreftelse = Bekreftelse(
                                    periodeId = request.perioder.first(),
                                    bekreftelsesloesning = Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING,
                                    id = UUID.randomUUID(),
                                    svar = Svar(
                                        sendtInnAv = Metadata(
                                            tidspunkt = Instant.now() - Duration.ofDays(1),
                                            utfoertAv = Bruker(
                                                type = Bruker.Type.VEILEDER,
                                                id = "12345678901",
                                                sikkerhetsnivaa = "idporten-loa-high"
                                            ),
                                            kilde = "dp",
                                            aarsak = "levert",
                                            tidspunktFraKilde = null
                                        ),
                                        gjelderFra = Instant.now() - Duration.ofDays(10),
                                        gjelderTil = Instant.now() - Duration.ofDays(5),
                                        harJobbetIDennePerioden = true,
                                        vilFortsetteSomArbeidssoeker = true
                                    )
                                )
                            )
                        )
                        )
                    )
//                    val tidslinjer = request.perioder.mapNotNull { periodeId ->
//                        val rader = transaction {
//                            hentForPeriode(periodeId)
//                        }
//                        genererTidslinje(periodeId, rader)
//                    }
//                    val identitetsnummer = tidslinjer.map { it.identitetsnummer }.toSet()
//                    (call.securityContext().bruker as Sluttbruker).alleIdenter
//                    BekreftelserResponse(
//                        bekreftelser = tidslinjer.flatMap { it.bekreftelser }
//                    )
                }
            }
        }
    }
}

fun autoriser(
    securityContext: SecurityContext,
    identitetsnummer: List<Identitetsnummer>,
    finnAlleIdenterForPerson: (Identitetsnummer) -> List<Identitetsnummer>,
    tilgangsTjenesteForAnsatte: TilgangsTjenesteForAnsatte
) {
    when (val bruker = securityContext.bruker) {
        is Anonym -> throw IllegalArgumentException("Anonym bruker har ikke tilgang til denne ressursen")
        is NavAnsatt -> autoriserAnsatt(identitetsnummer, tilgangsTjenesteForAnsatte)
        is Sluttbruker -> autoriserSluttbruker(bruker, identitetsnummer, finnAlleIdenterForPerson)
    }
}

fun autoriserSluttbruker(
    bruker: Sluttbruker,
    identitetsnummer: List<Identitetsnummer>,
    finnAlleIdenterForPerson: (Identitetsnummer) -> List<Identitetsnummer>
) {
    val alleIdenter = finnAlleIdenterForPerson(bruker.ident)
    if (identitetsnummer.any { it !in alleIdenter }) {
        throw IllegalArgumentException("Sluttbruker har ikke tilgang til alle identitetsnumre")
    }
}

fun autoriserAnsatt(
    identitetsnummer: List<Identitetsnummer>,
    tilgangsTjenesteForAnsatte: TilgangsTjenesteForAnsatte
) {
    TODO("Not yet implemented")
}

