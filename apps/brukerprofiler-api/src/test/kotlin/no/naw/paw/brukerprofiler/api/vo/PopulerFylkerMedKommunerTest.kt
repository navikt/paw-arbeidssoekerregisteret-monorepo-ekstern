package no.naw.paw.brukerprofiler.api.vo

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.naw.paw.brukerprofiler.kodeverk.SSBFylke
import no.naw.paw.brukerprofiler.kodeverk.SSBKommune

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
            apiFylker[0] shouldBe ApiFylke(
                navn = "Oslo",
                kommuner = listOf("Oslo"),
            )
            apiFylker[1] shouldBe ApiFylke(
                navn = "Troms",
                kommuner = listOf("Storfjord", "Tromsø"),
            )
        }

    }
})