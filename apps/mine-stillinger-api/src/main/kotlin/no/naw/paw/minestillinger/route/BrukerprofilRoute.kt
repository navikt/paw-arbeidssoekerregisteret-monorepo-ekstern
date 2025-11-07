package no.naw.paw.minestillinger.route

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import no.naw.paw.minestillinger.Clock
import no.naw.paw.minestillinger.api.ApiStillingssoek
import no.naw.paw.minestillinger.api.domain
import no.naw.paw.minestillinger.api.vo.toApiTjenesteStatus
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.brukerIkkeFunnet
import no.naw.paw.minestillinger.brukerprofil.hentApiBrukerprofil
import no.naw.paw.minestillinger.brukerprofil.setTjenestatestatus
import no.naw.paw.minestillinger.tjenesteIkkeAktiv
import no.naw.paw.minestillinger.db.ops.SøkAdminOps
import no.naw.paw.minestillinger.validering.valider
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import org.slf4j.LoggerFactory.getLogger

const val BRUKERPROFIL_PATH = "/api/v1/brukerprofil"

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
                val response = brukerprofilTjeneste.hentApiBrukerprofil(
                    hentSøk = { brukerId -> søkeAdminOps.hentSoek(brukerId).map { it.soek } },
                    identitetsnummer = identitetsnummer,
                )?.let(::Data) ?: brukerIkkeFunnet()
                call.respondWith(response)
            }
            put("/tjenestestatus/{tjenestestatus}") {
                val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                val tjenesteStatusParam = call.parameters["tjenestestatus"]
                    .toApiTjenesteStatus()
                val resultat = brukerprofilTjeneste.setTjenestatestatus(
                    identitetsnummer = identitetsnummer,
                    tjenesteStatus = tjenesteStatusParam
                )
                call.respondWith(resultat)
            }

            put<List<ApiStillingssoek>>("/stillingssoek") { stillingssoek ->
                val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                val validering = valider(stillingssoek)
                if (validering is ProblemDetails) {
                    call.respondWith(validering)
                    return@put
                } else {
                    val respose: Response<Unit> = suspendedTransactionAsync {
                        val profil = brukerprofilTjeneste.hentLokalBrukerProfilEllerNull(identitetsnummer)
                        val brukerId = profil?.id
                        if (brukerId == null) {
                            brukerIkkeFunnet()
                        } else {
                            if (!profil.tjenestenErAktiv) {
                                tjenesteIkkeAktiv()
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
}

fun SecurityContext.hentSluttbrukerEllerNull(): Sluttbruker? = (this.bruker as? Sluttbruker)
