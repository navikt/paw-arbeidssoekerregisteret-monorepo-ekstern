package no.naw.paw.minestillinger.validering

import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.naw.paw.minestillinger.api.ApiReiseveiSoek
import no.naw.paw.minestillinger.api.ApiStedSoek
import no.naw.paw.minestillinger.api.ApiStillingssoek
import no.naw.paw.minestillinger.reiseveiSøkIkkeStøttet

fun valider(stillingssoek: List<ApiStillingssoek>): Response<List<ApiStillingssoek>> {
    return stillingssoek
        .asSequence()
        .map { søk ->
            when (søk) {
                is ApiReiseveiSoek -> reiseveiSøkIkkeStøttet()
                is ApiStedSoek -> validerStedSoek(søk)
            }
        }
        .filterIsInstance<ProblemDetails>()
        .firstOrNull() ?: Data(stillingssoek)
}