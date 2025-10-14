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
import no.naw.paw.brukerprofiler.api.Brukerprofil
import no.naw.paw.brukerprofiler.api.Fylke
import no.naw.paw.brukerprofiler.api.ReiseveiSoek
import no.naw.paw.brukerprofiler.api.StedSoek
import no.naw.paw.brukerprofiler.api.StillingssoekType
import no.naw.paw.brukerprofiler.db.ops.hentBrukerProfil
import no.naw.paw.brukerprofiler.db.ops.setErIkkeInteressert
import no.naw.paw.brukerprofiler.db.ops.setTjenestenErAktiv

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

                //TODO: Dette er rart. kanTilbysTjenesten kaller hentBrukerprofil, og så kaller vi den på nytt for
                // å populere tjenestenErAktiv og erIkkeInteressert. Burde se på strukturen her.
                val kanTilbysTjenesten = brukerprofilTjeneste.kanTilbysTjenesten(identitetsnummer)
                val brukerProfil = hentBrukerProfil(identitetsnummer)


                val brukerprofil = Brukerprofil(
                    identitetsnummer = identitetsnummer.verdi,
                    kanTilbysTjenestenLedigeStillinger = kanTilbysTjenesten,
                    erTjenestenLedigeStillingerAktiv = brukerProfil!!.tjenestenErAktiv,
                    erIkkeInteressert = brukerProfil.erIkkeInteressert,
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
                        ReiseveiSoek(
                            soekType = StillingssoekType.REISEVEI_SOEK_V1,
                            maksAvstandKm = 42,
                            postnummer = "5145",
                            soekeord = listOf("Tryllekunstner"),
                        )
                    ),
                )
                call.respond(HttpStatusCode.OK, brukerprofil)
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