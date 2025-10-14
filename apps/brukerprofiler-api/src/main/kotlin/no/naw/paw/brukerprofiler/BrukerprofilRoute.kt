package no.naw.paw.brukerprofiler

import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.paw.security.authentication.model.SecurityContext
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import no.naw.paw.brukerprofiler.api.AvansertSoek
import no.naw.paw.brukerprofiler.api.Brukerprofil
import no.naw.paw.brukerprofiler.api.Fylke
import no.naw.paw.brukerprofiler.api.StedSoek
import no.naw.paw.brukerprofiler.api.StillingssoekType

const val BRUKERPROFIL_PATH = "/api/v1/brukerprofil"

fun Route.brukerprofilRoute(
    brukerprofilTjeneste: BrukerprofilTjeneste,
) {
    route(BRUKERPROFIL_PATH) {
        autentisering(TokenX) {
            get("") {
                val identitetsnummer = call.securityContext().hentSluttbrukerEllerNull()?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                val kanTilbysTjenesten = brukerprofilTjeneste.kanTilbysTjenesten(identitetsnummer)

                val brukerprofil = Brukerprofil(
                    identitetsnummer = identitetsnummer.verdi,
                    kanTilbysTjenestenLedigeStillinger = kanTilbysTjenesten,
                    erTjenestenLedigeStillingerAktiv = false,
                    erIkkeInteressert = false,
                    stillingssoek = listOf(
                        StedSoek(
                            soekType = StillingssoekType.STED_SOEK_V1,
                            fylker = listOf(
                                Fylke("BUSKERUD", kommuner = listOf("DRAMMEN", "KONGSBERG")),
                                Fylke("VESTLAND", kommuner = listOf("BERGEN")),
                                Fylke("BODØ", kommuner = listOf("TROMSØ")),
                                Fylke("FINNMARK", kommuner = emptyList())
                            ),
                            soekeord = listOf("Tryllekunstner"),
                        ),
                        AvansertSoek(
                            soekType = StillingssoekType.AVANSERT_SOEK_V1,
                            hugga = 42,
                            styrk08 = "1234"
                        )
                    ),
                )
                call.respond(HttpStatusCode.OK, brukerprofil)
            }
        }
    }
}

fun SecurityContext.hentSluttbrukerEllerNull(): Sluttbruker? = (this.bruker as? Sluttbruker)