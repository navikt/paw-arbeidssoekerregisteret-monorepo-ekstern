package no.naw.paw.minestillinger.route

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import no.naw.paw.minestillinger.BrukerprofilTjeneste
import no.naw.paw.minestillinger.api.ApiStillingssoek
import no.naw.paw.minestillinger.api.domain
import no.naw.paw.minestillinger.api.vo.toApiTjenesteStatus
import no.naw.paw.minestillinger.db.ops.hentBrukerProfil
import no.naw.paw.minestillinger.db.ops.hentSoek
import no.naw.paw.minestillinger.db.ops.lagreSoek
import no.naw.paw.minestillinger.db.ops.setTjenestatus
import no.naw.paw.minestillinger.db.ops.slettAlleSoekForBruker
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.Stillingssoek
import no.naw.paw.minestillinger.domain.TjenesteStatus
import no.naw.paw.minestillinger.domain.api
import no.naw.paw.minestillinger.domain.feilVedForsøkPåÅSetteKanIkkeLeveres
import no.naw.paw.minestillinger.domain.toTjenesteStatus
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

const val BRUKERPROFIL_PATH = "/api/v1/brukerprofil"

fun Route.brukerprofilRoute(
    brukerprofilTjeneste: BrukerprofilTjeneste,
) {
    route(BRUKERPROFIL_PATH) {
        autentisering(TokenX) {
            get("") {
                val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")

                val lagretProfil = hentBrukerProfil(identitetsnummer)
                val oppdatertApiBrukerprofiler =
                    lagretProfil
                        ?.suspendedLet(brukerprofilTjeneste::oppdaterKanTilbysTjenesten)
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
            }
            put("/tjenestestatus/{tjenestestatus}") {
                val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                val tjenesteStatus = call.parameters["tjenestestatus"]
                    .toApiTjenesteStatus()
                    .toTjenesteStatus()
                    .feilVedForsøkPåÅSetteKanIkkeLeveres()

                val brukerProfil = hentBrukerProfil(identitetsnummer)
                when {
                    brukerProfil == null -> call.respond(HttpStatusCode.NotFound)
                    brukerProfil.tjenestestatus == TjenesteStatus.KAN_IKKE_LEVERES -> call.respond(HttpStatusCode.Forbidden)
                    else -> {
                        setTjenestatus(identitetsnummer, tjenesteStatus)
                        call.respond(HttpStatusCode.NoContent)
                    }
                }
            }

            put<List<ApiStillingssoek>>("/stillingssoek") { stillingssoek ->
                val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                val httpCode = transaction {
                    val brukerId = hentBrukerProfil(identitetsnummer)?.id
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
                }
                call.respond(httpCode)
            }
        }
    }
}

fun SecurityContext.hentSluttbrukerEllerNull(): Sluttbruker? = (this.bruker as? Sluttbruker)

suspend fun <T1, R> T1.suspendedLet(transform: suspend (T1) -> R): R = transform(this)