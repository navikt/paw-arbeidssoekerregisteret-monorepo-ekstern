package no.naw.paw.minestillinger.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.paw.error.exception.ProblemDetailsException
import no.nav.paw.error.model.ErrorType
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import no.naw.paw.minestillinger.api.ApiStillingssoek
import no.naw.paw.minestillinger.api.domain
import no.naw.paw.minestillinger.api.vo.toApiTjenesteStatus
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.brukerprofil.flagg.OppdateringAvFlagg
import no.naw.paw.minestillinger.brukerprofil.kanTilbysTjenesten
import no.naw.paw.minestillinger.db.ops.hentSoek
import no.naw.paw.minestillinger.db.ops.lagreSoek
import no.naw.paw.minestillinger.db.ops.slettAlleSoekForBruker
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.brukerprofil.flagg.OptOutFlag
import no.naw.paw.minestillinger.domain.Stillingssoek
import no.naw.paw.minestillinger.domain.TjenesteStatus
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlagg
import no.naw.paw.minestillinger.domain.api
import no.naw.paw.minestillinger.domain.feilVedForsøkPåÅSetteKanIkkeLeveres
import no.naw.paw.minestillinger.domain.toTjenesteStatus
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory.getLogger
import java.time.Instant
import java.util.UUID
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

const val BRUKERPROFIL_PATH = "/api/v1/brukerprofil"
private val logger = getLogger(BRUKERPROFIL_PATH)

fun Route.brukerprofilRoute(
    brukerprofilTjeneste: BrukerprofilTjeneste,
) {
    route(BRUKERPROFIL_PATH) {
        autentisering(TokenX) {
            get("") {
                suspendedTransactionAsync {
                    val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                        ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")

                    val lagretProfil = brukerprofilTjeneste.hentBrukerProfil(identitetsnummer)
                    val oppdatertApiBrukerprofiler =
                        lagretProfil
                            ?.let(BrukerProfil::api)
                            ?.let { profil ->
                                profil.copy(
                                    stillingssoek = transaction {
                                        hentSoek(lagretProfil.id)
                                            .map { it.soek }
                                            .map(Stillingssoek::api)
                                    }
                                )
                            }
                    if (oppdatertApiBrukerprofiler != null) {
                        call.respond(oppdatertApiBrukerprofiler)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }.await()
            }
            put("/tjenestestatus/{tjenestestatus}") {
                suspendedTransactionAsync {
                    val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                        ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                    val tjenesteStatus = call.parameters["tjenestestatus"]
                        .toApiTjenesteStatus()
                        .toTjenesteStatus()
                        .feilVedForsøkPåÅSetteKanIkkeLeveres()
                    val brukerProfil =
                        brukerprofilTjeneste.hentBrukerProfil(identitetsnummer) ?: throw ProblemDetailsException(
                            problemDetails(
                                operasjon = call.request.uri,
                                httpStatusCode = HttpStatusCode.NotFound,
                                feilTittel = "Brukerprofil ikke funnet",
                                feilBeskrivelse = "Brukerprofil for sluttbruker ikke funnet",
                                instance = call.request.uri
                            )
                        )
                    val oppdatering = when (tjenesteStatus) {
                        TjenesteStatus.AKTIV -> {
                            if (!kanTilbysTjenesten(brukerProfil)) {
                                throw ProblemDetailsException(
                                    details = problemDetails(
                                        operasjon = call.request.uri,
                                        httpStatusCode = HttpStatusCode.Forbidden,
                                        feilTittel = "Tjenesten kan ikke leveres",
                                        feilBeskrivelse = brukerProfil.toString(),
                                        instance = call.request.uri
                                    )
                                )
                            }
                            OppdateringAvFlagg(
                                nyeOgOppdaterteFlagg = listOf(
                                    TjenestenErAktivFlagg(true, Instant.now()),
                                    OptOutFlag(false, Instant.now())
                                ),
                                søkSkalSlettes = false
                            )
                        }

                        TjenesteStatus.INAKTIV -> OppdateringAvFlagg(
                            nyeOgOppdaterteFlagg = listOf(TjenestenErAktivFlagg(false, Instant.now())),
                            søkSkalSlettes = false
                        )

                        TjenesteStatus.OPT_OUT -> OppdateringAvFlagg(
                            nyeOgOppdaterteFlagg = listOf(OptOutFlag(true, Instant.now())),
                            søkSkalSlettes = true
                        )

                        TjenesteStatus.KAN_IKKE_LEVERES -> {
                            throw ProblemDetailsException(
                                details = problemDetails(
                                    operasjon = call.request.uri,
                                    httpStatusCode = HttpStatusCode.BadRequest,
                                    feilTittel = "Ugyldig tjenestestatus",
                                    feilBeskrivelse = brukerProfil.toString(),
                                    instance = call.request.uri
                                )
                            )
                        }
                    }
                    logger.info("inn_tjenestestatus=${tjenesteStatus}, ut_oppdatering=${oppdatering}")
                    brukerprofilTjeneste.oppdaterFlagg(brukerProfil.id, oppdatering)
                }.await()
                call.respond(HttpStatusCode.NoContent)
            }

            put<List<ApiStillingssoek>>("/stillingssoek") { stillingssoek ->
                val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                val httpCode = suspendedTransactionAsync {
                    val brukerId = brukerprofilTjeneste.hentBrukerProfil(identitetsnummer)?.id
                    if (brukerId == null) {
                        HttpStatusCode.NotFound
                    } else {
                        slettAlleSoekForBruker(brukerId)
                        val tidspunkt = Instant.now()
                        stillingssoek.forEach { soek ->
                            lagreSoek(brukerId, tidspunkt, soek.domain())
                        }
                        HttpStatusCode.NoContent
                    }
                }.await()
                call.respond(httpCode)
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
private fun RoutingContext.validerRequest(path: String, brukerProfil: BrukerProfil?) {
    contract {
        returns() implies (brukerProfil != null)
    }
    when {
        brukerProfil == null -> throw ProblemDetailsException(
            details = problemDetails(
                operasjon = path,
                httpStatusCode = HttpStatusCode.NotFound,
                feilTittel = "Brukerprofil ikke funnet",
                feilBeskrivelse = "Brukerprofil for sluttbruker ikke funnet",
                instance = path
            )
        )

        brukerProfil.harGradertAdresse -> {
            throw ProblemDetailsException(
                details = problemDetails(
                    operasjon = path,
                    httpStatusCode = HttpStatusCode.Forbidden,
                    feilTittel = "Tjenesten kan ikke leveres",
                    feilBeskrivelse = "Brukeren kan ikke benytte tjenesten",
                    instance = path
                )
            )
        }

        !brukerProfil.erITestGruppen -> {
            throw ProblemDetailsException(
                details = problemDetails(
                    operasjon = path,
                    httpStatusCode = HttpStatusCode.Forbidden,
                    feilTittel = "Tjenesten kan ikke leveres",
                    feilBeskrivelse = "Brukeren kan ikke benytte tjenesten",
                    instance = path
                )
            )
        }

        !brukerProfil.harBruktTjenesten && !brukerProfil.harGodeMuligheter -> {
            throw ProblemDetailsException(
                details = problemDetails(
                    operasjon = path,
                    httpStatusCode = HttpStatusCode.Forbidden,
                    feilTittel = "Tjenesten kan ikke leveres",
                    feilBeskrivelse = "Brukeren kan ikke benytte tjenesten",
                    instance = path
                )
            )
        }
    }
}

fun SecurityContext.hentSluttbrukerEllerNull(): Sluttbruker? = (this.bruker as? Sluttbruker)

fun problemDetails(
    operasjon: String,
    httpStatusCode: HttpStatusCode,
    feilTittel: String,
    feilBeskrivelse: String,
    instance: String
): ProblemDetails = ProblemDetails(
    id = UUID.randomUUID(),
    type = ErrorType
        .domain("mine-stillinger")
        .error(operasjon)
        .build(),
    status = httpStatusCode,
    title = feilTittel,
    detail = feilBeskrivelse,
    instance = instance,
    timestamp = Instant.now()
)