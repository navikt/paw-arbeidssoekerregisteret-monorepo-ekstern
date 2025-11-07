package no.naw.paw.minestillinger.validering

import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.naw.paw.minestillinger.ugyldigVerdiIRequest

fun validerSøkeord(ord: List<String>): Response<Unit> {
    return ord.fold(ok) { acc, søkeord ->
        acc.map {
            if (søkeordRegex.matches(søkeord)) it else ugyldigVerdiIRequest(felt = "soekeord", verdi = søkeord)
        }
    }
}