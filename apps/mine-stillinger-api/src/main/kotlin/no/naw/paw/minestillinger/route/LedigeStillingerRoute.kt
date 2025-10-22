package no.naw.paw.minestillinger.route

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.paw.security.authentication.model.TokenX
import no.nav.paw.security.authentication.model.securityContext
import no.nav.paw.security.authentication.plugin.autentisering
import no.naw.paw.ledigestillinger.model.FinnStillingerRequest
import no.naw.paw.ledigestillinger.model.Kommune
import no.naw.paw.ledigestillinger.model.Paging
import no.naw.paw.ledigestillinger.model.SortOrder
import no.naw.paw.minestillinger.FinnStillingerClient
import no.naw.paw.minestillinger.api.JobbAnnonse
import no.naw.paw.minestillinger.db.ops.hentBrukerProfil
import no.naw.paw.minestillinger.db.ops.hentSoek
import no.naw.paw.minestillinger.domain.StedSoek

fun Route.ledigeStillingerRoute(ledigeStillingerClient: FinnStillingerClient) {
    route("/api/v1/ledigestillinger") {
        autentisering(TokenX) {
            get {
                val identitetsnummer = call.securityContext()
                    .hentSluttbrukerEllerNull()
                    ?.ident
                    ?: throw BadRequestException("Kun støtte for tokenX (sluttbrukere)")
                val profil = hentBrukerProfil(identitetsnummer)
                val soek = profil?.let { profil -> hentSoek(profil.id) }
                    ?.firstOrNull { it.soek is StedSoek}
                val request = soek?.let { stedSøk ->
                    val søk = stedSøk.soek as StedSoek
                    FinnStillingerRequest(
                        soekeord = søk.soekeord,
                        kategorier = søk.styrk08,
                        fylker = søk.fylker.map { fylke ->
                            no.naw.paw.ledigestillinger.model.Fylke(
                                fylkesnummer = fylke.fylkesnummer,
                                kommuner = fylke.kommuner.map { kommune ->
                                    Kommune(kommunenummer = kommune.kommunenummer)
                                }
                            )
                        },
                        paging = Paging(page = 1, pageSize = 30, sortOrder = SortOrder.ASC)
                    )
                }
                if (request != null) {
                    val response = ledigeStillingerClient.finnLedigeStillinger(request)
                    response.stillinger?.map { stilling ->
                        JobbAnnonse(
                            tittel = stilling.tittel,
                            stillingbeskrivelse = stilling.jobbtittel,
                            publisert = stilling.publisert,
                            soeknadsfrist = TODO(),
                            land = TODO(),
                            kommune = TODO(),
                            sektor = TODO(),
                            selskap = stilling.arbeidsgiver ?: "Ukjent"

                        )
                    }

                }
            }
        }
    }
}
