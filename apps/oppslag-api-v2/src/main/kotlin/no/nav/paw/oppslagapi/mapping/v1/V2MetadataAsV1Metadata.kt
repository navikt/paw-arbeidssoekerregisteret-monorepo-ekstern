package no.nav.paw.oppslagapi.mapping.v1

import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.MetadataResponse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Metadata

fun Metadata.v1Metadata(): MetadataResponse {
    return MetadataResponse(
        tidspunkt = tidspunkt,
        utfoertAv = utfoertAv.v1Bruker(),
        kilde = kilde,
        aarsak = aarsak,
        tidspunktFraKilde = tidspunktFraKilde?.v1TidspunktFraKilde()
    )
}
