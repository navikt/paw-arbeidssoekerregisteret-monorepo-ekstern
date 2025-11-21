package no.nav.paw.oppslagapi.mapping.v1

import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.BrukerResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.BrukerType
import no.nav.paw.oppslagapi.model.v2.Bruker

fun Bruker.v1Bruker(): BrukerResponse {
    return BrukerResponse(
        type = when (type) {
            no.nav.paw.oppslagapi.model.v2.BrukerType.UKJENT_VERDI -> BrukerType.UKJENT_VERDI
            no.nav.paw.oppslagapi.model.v2.BrukerType.UDEFINERT -> BrukerType.UDEFINERT
            no.nav.paw.oppslagapi.model.v2.BrukerType.VEILEDER -> BrukerType.VEILEDER
            no.nav.paw.oppslagapi.model.v2.BrukerType.SYSTEM -> BrukerType.SYSTEM
            no.nav.paw.oppslagapi.model.v2.BrukerType.SLUTTBRUKER -> BrukerType.SLUTTBRUKER
        },
        id = id
    )
}