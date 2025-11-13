package no.nav.paw.oppslagapi.mapping.v1

import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.AvviksTypeResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.TidspunktFraKildeResponse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.AvviksType
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.TidspunktFraKilde

fun TidspunktFraKilde.v1TidspunktFraKilde(): TidspunktFraKildeResponse {
    return TidspunktFraKildeResponse(
        tidspunkt = tidspunkt,
        avviksType = when (avviksType) {
            AvviksType.UKJENT_VERDI -> AvviksTypeResponse.UKJENT_VERDI
            AvviksType.FORSINKELSE -> AvviksTypeResponse.FORSINKELSE
            AvviksType.RETTING -> AvviksTypeResponse.RETTING
            AvviksType.SLETTET -> AvviksTypeResponse.SLETTET
            AvviksType.TIDSPUNKT_KORRIGERT -> AvviksTypeResponse.TIDSPUNKT_KORRIGERT
        }
    )
}