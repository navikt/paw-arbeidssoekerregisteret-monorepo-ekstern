package no.naw.paw.brukerprofiler

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import no.naw.paw.brukerprofiler.api.Brukerprofil
import no.naw.paw.brukerprofiler.api.SimpeltSoek
import no.naw.paw.brukerprofiler.api.StillingssoekType

const val BRUKERPROFIL_PATH = "/api/v1/brukerprofil"

fun Route.brukerprofilRoute(
    brukerprofilTjeneste: BrukerprofilTjeneste,
) {
    autentisering(TokenX) {
        get(BRUKERPROFIL_PATH) {
            val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
            val kanTilbysTjenesten = brukerprofilTjeneste.kanTilbysTjenesten(identitetsnummer)

            val brukerprofil = Brukerprofil(
                identitetsnummer = identitetsnummer.verdi,
                kanTilbysTjenestenLedigeStillinger = kanTilbysTjenesten,
                erTjenestenLedigeStillingerAktiv = false,
                stillingssoek = listOf(
                    SimpeltSoek(
                        soekType = StillingssoekType.SIMPELT_SOEK_V1,
                        kommune = "3057",
                        styrk08 = "1234"
                    )
                ),
            )
            call.respond(HttpStatusCode.OK, brukerprofil)
        }
    }
}

fun SecurityContext.hentSluttbrukerEllerNull(): Sluttbruker? = (this.bruker as? Sluttbruker)