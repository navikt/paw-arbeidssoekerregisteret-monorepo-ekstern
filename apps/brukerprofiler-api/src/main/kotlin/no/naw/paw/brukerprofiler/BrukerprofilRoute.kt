package no.naw.paw.brukerprofiler

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
import no.naw.paw.brukerprofiler.db.ops.hentBrukerProfil
import no.naw.paw.brukerprofiler.db.ops.setErIkkeInteressert
import no.naw.paw.brukerprofiler.db.ops.setTjenestenErAktiv
import no.naw.paw.brukerprofiler.domain.BrukerProfil
import no.naw.paw.brukerprofiler.domain.api

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

                val oppdatertApiBrukerprofiler = hentBrukerProfil(identitetsnummer)
                    ?.suspendedLet(brukerprofilTjeneste::oppdaterKanTilbysTjenesten)
                    ?.let(BrukerProfil::api)
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
        }
    }
}

fun SecurityContext.hentSluttbrukerEllerNull(): Sluttbruker? = (this.bruker as? Sluttbruker)

suspend fun <T1, R> T1.suspendedLet(transform: suspend (T1) -> R): R = transform(this)