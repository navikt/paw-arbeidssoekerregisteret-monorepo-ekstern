package no.naw.paw.minestillinger

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
import no.naw.paw.minestillinger.api.ApiStillingssoek
import no.naw.paw.minestillinger.api.domain
import no.naw.paw.minestillinger.db.ops.hentBrukerProfil
import no.naw.paw.minestillinger.db.ops.hentSoek
import no.naw.paw.minestillinger.db.ops.lagreSoek
import no.naw.paw.minestillinger.db.ops.setErIkkeInteressert
import no.naw.paw.minestillinger.db.ops.setTjenestenErAktiv
import no.naw.paw.minestillinger.db.ops.slettAlleSoekForBruker
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.Stillingssoek
import no.naw.paw.minestillinger.domain.api
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

const val BRUKERPROFIL_PATH = "/api/v1/brukerprofil"
const val ER_TJENESTEN_AKTIV_PATH = "$BRUKERPROFIL_PATH/erTjenestenLedigeStillingerAktiv"
const val ER_IKKE_INTERESSERT_PATH = "$BRUKERPROFIL_PATH/erIkkeInteressert"

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
            put("/erTjenestenLedigeStillingerAktiv/{aktiv}") {
                val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                val erTjenestenAktiv = call.parameters["aktiv"]?.toBooleanStrictOrNull()
                    ?: throw BadRequestException("Ugyldig verdi for param 'aktiv'")

                setTjenestenErAktiv(identitetsnummer, erTjenestenAktiv)
                call.respond(HttpStatusCode.NoContent)
            }
            put("/erIkkeInteressert/{erIkkeInteressert}") {
                val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                val erIkkeInteressert = call.parameters["erIkkeInteressert"]?.toBooleanStrictOrNull()
                    ?: throw BadRequestException("Ugyldig verdi for param 'erIkkeInteressert'")

                setErIkkeInteressert(identitetsnummer, erIkkeInteressert)
                call.respond(HttpStatusCode.NoContent)
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