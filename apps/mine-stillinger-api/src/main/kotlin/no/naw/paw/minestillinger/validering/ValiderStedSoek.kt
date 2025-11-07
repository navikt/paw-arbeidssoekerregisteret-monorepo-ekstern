package no.naw.paw.minestillinger.validering

import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.naw.paw.minestillinger.api.ApiStedSoek

fun validerStedSoek(apiStedSoek: ApiStedSoek): Response<ApiStedSoek> {
    return sequenceOf(
        { validerStyrkListe(apiStedSoek.styrk08) },
        { validerFylkerOgKommuner(apiStedSoek.fylker) },
        { validerSøkeord(apiStedSoek.soekeord) }
    ).filterIsInstance<ProblemDetails>()
        .firstOrNull() ?: return Data(apiStedSoek)
}