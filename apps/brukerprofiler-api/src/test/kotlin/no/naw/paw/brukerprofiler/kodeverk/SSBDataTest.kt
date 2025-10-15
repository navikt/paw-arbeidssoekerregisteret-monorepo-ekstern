package no.naw.paw.brukerprofiler.kodeverk

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class SSBDataTest : FreeSpec({
    "Splitter fylker riktig" {
        SSBFylke(
            code = "01",
            name = "Oslo - Kristiania",
        ) should {
            it.nameList.size shouldBe 2
            it.nameList shouldContain "Oslo"
            it.nameList shouldContain "Kristiania"
        }

        SSBFylke(
            code = "04",
            name = "Møre og Romsdal"
        ) should {
            it.nameList.size shouldBe 1
            it.nameList shouldContain "Møre og Romsdal"
        }
    }

    "Splitter kommuner riktig" {
        SSBKommune(
            code = "5538",
            name = "Storfjord - Omasvuotna - Omasvuono"
        ) should {
            it.nameList.size shouldBe 3
            it.nameList shouldContain "Storfjord"
            it.nameList shouldContain "Omasvuotna"
            it.nameList shouldContain "Omasvuono"
            it.fylkesnummer shouldBe "55"
        }

        SSBKommune(
            code = "0426",
            name = "Stor-Elvdal",
        ) should {
            it.nameList.size shouldBe 1
            it.nameList shouldContain "Stor-Elvdal"
            it.fylkesnummer shouldBe "04"
        }

        SSBKommune(
            code = "0231",
            name = "Herøy (Møre og Romsdal)"
        ) should {
            it.nameList.size shouldBe 1
            it.nameList shouldContain "Herøy (Møre og Romsdal)"
            it.fylkesnummer shouldBe "02"
        }
    }
})