package no.naw.paw.minestillinger.api.vo

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.naw.paw.minestillinger.kodeverk.SSBFylke
import no.naw.paw.minestillinger.kodeverk.SSBKommune

class PopulerFylkerMedKommunerTest : FreeSpec({

    "Kan kombinere SSBFylker og SSBKommuner til ApiFylker" {
        populerFylkerMedKommuner(
            fylker = listOf(
                SSBFylke(fylkesnummer = "03", name = "Oslo"),
                SSBFylke(fylkesnummer = "55", name = "Troms - Romsa - Tromssa"),
            ),
            kommuner = listOf(
                SSBKommune(kommunenummer = "0301", name = "Oslo"),
                SSBKommune(kommunenummer = "5538", name = "Storfjord - Omasvuotna - Omasvuono"),
                SSBKommune(kommunenummer = "5501", name = "Tromsø"),
            )
        ) should { apiFylker ->
            apiFylker.size shouldBe 2
            apiFylker[0] should { fylke ->
                fylke.navn shouldBe "Oslo"
                fylke.kommuner shouldContainExactly listOf(ApiKommune(kommunenummer = "0301", navn = "Oslo"))
                fylke.fylkesnummer shouldBe "03"
            }
            apiFylker[1] should { fylke ->
                fylke.navn shouldBe "Troms"
                fylke.fylkesnummer shouldBe "55"
                fylke.kommuner shouldContainExactly listOf(
                    ApiKommune(navn = "Storfjord", kommunenummer = "5538"),
                    ApiKommune(navn = "Tromsø", kommunenummer = "5501"),
                )
            }
        }

    }
})