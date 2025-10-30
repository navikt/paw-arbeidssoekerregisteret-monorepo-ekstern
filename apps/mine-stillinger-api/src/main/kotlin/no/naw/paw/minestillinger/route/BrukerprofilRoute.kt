package no.naw.paw.minestillinger.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.Response
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import no.naw.paw.minestillinger.Clock
import no.naw.paw.minestillinger.Error
import no.naw.paw.minestillinger.api.ApiStillingssoek
import no.naw.paw.minestillinger.api.domain
import no.naw.paw.minestillinger.api.vo.toApiTjenesteStatus
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.brukerprofil.hentBrukerprofil
import no.naw.paw.minestillinger.brukerprofil.setTjenestatestatus
import no.naw.paw.minestillinger.brukerprofil.tjenestestatus
import no.naw.paw.minestillinger.db.ops.SøkAdminOps
import no.naw.paw.minestillinger.domain.TjenesteStatus
import no.naw.paw.minestillinger.mineStillingerProblemDetails
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import org.slf4j.LoggerFactory.getLogger

const val BRUKERPROFIL_PATH = "/api/v1/brukerprofil"
private val logger = getLogger(BRUKERPROFIL_PATH)

fun Route.brukerprofilRoute(
    brukerprofilTjeneste: BrukerprofilTjeneste,
    søkeAdminOps: SøkAdminOps,
    clock: Clock
) {
    route(BRUKERPROFIL_PATH) {
        autentisering(TokenX) {
            get("") {
                val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                val response = brukerprofilTjeneste.hentBrukerprofil(
                    hentSøk = { brukerId -> søkeAdminOps.hentSoek(brukerId).map { it.soek } },
                    identitetsnummer = identitetsnummer,
                )?.let(::Data) ?: mineStillingerProblemDetails(
                    error = Error.BRUKERPROFIL_IKKE_FUNNET,
                    detail = "Brukerprofil ikke funnet",
                    instance = call.request.uri
                )
                call.respondWith(response)
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
                val respose: Response<Unit> = suspendedTransactionAsync {
                    val profil = brukerprofilTjeneste.hentBrukerProfil(identitetsnummer)
                    val brukerId = profil?.id
                    if (brukerId == null) {
                        mineStillingerProblemDetails(
                            error = Error.BRUKERPROFIL_IKKE_FUNNET,
                            detail = "Brukerprofil ikke funnet",
                            instance = call.request.uri
                        )
                    } else {
                        if (profil.listeMedFlagg.tjenestestatus() != TjenesteStatus.AKTIV) {
                            mineStillingerProblemDetails(
                                error = Error.TJENESTEN_ER_IKKE_AKTIV,
                                detail = "Kan ikke oppdatere stillingssøk når tjenesten ikke er aktiv for brukeren.",
                                instance = call.request.uri
                            )
                        } else {
                            søkeAdminOps.slettAlleSoekForBruker(brukerId)
                            val tidspunkt = clock.now()
                            stillingssoek.forEach { soek ->
                                søkeAdminOps.lagreSoek(brukerId, tidspunkt, soek.domain())
                            }
                            Data(Unit)
                        }
                    }
                }.await()
                call.respondWith(respose)
            }
        }
    }
}

fun SecurityContext.hentSluttbrukerEllerNull(): Sluttbruker? = (this.bruker as? Sluttbruker)

