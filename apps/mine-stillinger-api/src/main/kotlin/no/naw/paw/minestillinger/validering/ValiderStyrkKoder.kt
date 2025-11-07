package no.naw.paw.minestillinger.validering

import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.naw.paw.minestillinger.ugyldigVerdiIRequest


fun validerStyrkListe(styrk08: List<String>): Response<Unit> {
    return styrk08.fold(ok) { acc, styrk ->
        acc.map {
            if (styrkRegex.matches(styrk)) it else ugyldigVerdiIRequest(felt = "styrk08", verdi = styrk)
        }
    }
}