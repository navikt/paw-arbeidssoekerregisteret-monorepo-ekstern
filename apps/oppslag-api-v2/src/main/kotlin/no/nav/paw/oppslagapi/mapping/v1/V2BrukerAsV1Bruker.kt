package no.nav.paw.oppslagapi.mapping.v1

import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.BrukerResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.BrukerType
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bruker

fun Bruker.v1Bruker(): BrukerResponse {
    return BrukerResponse(
        type = when (type) {
            Bruker.Type.UKJENT_VERDI -> BrukerType.UKJENT_VERDI
            Bruker.Type.UDEFINERT -> BrukerType.UDEFINERT
            Bruker.Type.VEILEDER -> BrukerType.VEILEDER
            Bruker.Type.SYSTEM -> BrukerType.SYSTEM
            Bruker.Type.SLUTTBRUKER -> BrukerType.SLUTTBRUKER
        },
        id = id
    )
}