package no.naw.paw.minestillinger

import no.naw.paw.minestillinger.api.ApiStedSoek
import no.naw.paw.minestillinger.api.vo.ApiFylke
import no.naw.paw.minestillinger.api.vo.ApiKommune
import no.naw.paw.minestillinger.api.vo.ApiStillingssoekType

fun createApiStedSoek(
    fylkeNavn: String,
    fylkesnummer: String,
    kommuneNavn: String,
    kommunenummer: String
) = ApiStedSoek(
    soekType = ApiStillingssoekType.STED_SOEK_V1,
    fylker = listOf(
        ApiFylke(
            navn = fylkeNavn,
            fylkesnummer = fylkesnummer,
            kommuner = listOf(
                ApiKommune(
                    navn = kommuneNavn,
                    kommunenummer = kommunenummer
                )
            )
        )
    ),
    soekeord = emptyList(),
    styrk08 = emptyList()
)