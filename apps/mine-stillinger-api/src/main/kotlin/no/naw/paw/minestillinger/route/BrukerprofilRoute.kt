package no.naw.paw.minestillinger.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
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
import no.naw.paw.minestillinger.brukerprofil.hentBrukerprofil
import no.naw.paw.minestillinger.brukerprofil.setTjenestatestatus
import no.naw.paw.minestillinger.db.ops.SøkAdminOps
import no.naw.paw.minestillinger.domain.BrukerProfil
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import org.slf4j.LoggerFactory.getLogger
import java.time.Instant
import java.util.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

const val BRUKERPROFIL_PATH = "/api/v1/brukerprofil"
private val logger = getLogger(BRUKERPROFIL_PATH)

fun Route.brukerprofilRoute(
    brukerprofilTjeneste: BrukerprofilTjeneste,
    søkeAdminOps: SøkAdminOps
) {
    route(BRUKERPROFIL_PATH) {
        autentisering(TokenX) {
            get("") {
                val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                val apiBrukerprofil = brukerprofilTjeneste.hentBrukerprofil(
                    hentSøk = { brukerId -> søkeAdminOps.hentSoek(brukerId).map { it.soek } },
                    identitetsnummer = identitetsnummer,
                )
                call.respond(apiBrukerprofil)
            }
            put("/tjenestestatus/{tjenestestatus}") {
                val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                val tjenesteStatusParam = call.parameters["tjenestestatus"]
                    .toApiTjenesteStatus()
                brukerprofilTjeneste.setTjenestatestatus(
                    identitetsnummer = identitetsnummer,
                    tjenesteStatus = tjenesteStatusParam
                )
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
                        søkeAdminOps.slettAlleSoekForBruker(brukerId)
                        val tidspunkt = Instant.now()
                        stillingssoek.forEach { soek ->
                            søkeAdminOps.lagreSoek(brukerId, tidspunkt, soek.domain())
                        }
                        HttpStatusCode.NoContent
                    }
                }.await()
                call.respond(httpCode)
            }
        }
    }
}

fun SecurityContext.hentSluttbrukerEllerNull(): Sluttbruker? = (this.bruker as? Sluttbruker)
