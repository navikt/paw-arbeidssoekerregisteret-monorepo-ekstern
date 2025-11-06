package no.naw.paw.minestillinger

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.naw.paw.minestillinger.api.ApiStedSoek

fun assertStedSoek(
    søk: ApiStedSoek,
    fylkeNavn: String,
    fylkesnummer: String,
    kommuneNavn: String,
    kommunenummer: String
) {
    søk.fylker.firstOrNull() should { fylke ->
        fylke.shouldNotBeNull()
        fylke.fylkesnummer shouldBe fylkesnummer
        fylke.navn shouldBe fylkeNavn
        fylke.kommuner.firstOrNull() should { kommune ->
            kommune.shouldNotBeNull()
            kommune.kommunenummer shouldBe kommunenummer
            kommune.navn shouldBe kommuneNavn
        }
    }
}