package no.nav.paw.arbeidssoekerregisteret.utils

import no.nav.paw.security.authentication.model.Bruker
import no.nav.paw.security.authentication.model.Identitetsnummer
import no.nav.paw.security.authentication.model.M2MToken
import no.nav.paw.security.authentication.model.NavAnsatt
import no.nav.paw.security.authentication.model.Sluttbruker
import no.nav.paw.security.authentication.model.asIdentitetsnummer
import no.nav.paw.security.authorization.exception.IngenTilgangException

fun Bruker<*>.hentSluttbrukerIdentitet(): Identitetsnummer {
    return when (this) {
        is Sluttbruker -> ident
        else -> throw IngenTilgangException("Endepunkt kan kun benyttes av sluttbruker")
    }
}
