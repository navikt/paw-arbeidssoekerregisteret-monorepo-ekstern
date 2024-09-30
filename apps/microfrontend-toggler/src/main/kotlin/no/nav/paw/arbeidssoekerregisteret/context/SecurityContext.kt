package no.nav.paw.arbeidssoekerregisteret.context

import no.nav.paw.arbeidssoekerregisteret.model.AccessToken
import no.nav.paw.arbeidssoekerregisteret.model.InnloggetBruker

data class SecurityContext(
    val innloggetBruker: InnloggetBruker,
    val accessToken: AccessToken
)
