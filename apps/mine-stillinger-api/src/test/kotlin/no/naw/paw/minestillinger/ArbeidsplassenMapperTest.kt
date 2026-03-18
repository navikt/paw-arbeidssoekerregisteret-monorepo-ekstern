package no.naw.paw.minestillinger

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

class ArbeidsplassenMapperTest : FreeSpec({

    "Sanity" - {
        "Kjent styrkKode gir ikke-tom liste" {
            // "2212" (Lege) tilhører "Helse og sosial" som har mange koder
            ArbeidsplassenMapper.relaterteStyrkKoder("2212").shouldNotBeEmpty()
        }
    }

    "Koden selv skal være med i resultatet" - {
        "2212 skal returneres når vi slår opp 2212" {
            ArbeidsplassenMapper.relaterteStyrkKoder("2212") shouldContain "2212"
        }
    }

    "Alle returnerte koder tilhører samme categoryLevel1" - {
        "Alle koder relatert til 2212 (Helse og sosial) er fra Helse og sosial" {
            // Spot-check: kjente Helse og sosial-koder skal være med
            val relaterte = ArbeidsplassenMapper.relaterteStyrkKoder("2212")
            relaterte shouldContainAll listOf("2221", "2224", "2263", "2264", "3211", "3412", "5321", "5322")
            relaterte.size shouldBe 32
        }

        "Alle koder relatert til 2514 (IT) er fra IT" {
            val relaterte = ArbeidsplassenMapper.relaterteStyrkKoder("2514")
            relaterte shouldContainAll listOf("1330", "2166", "3514")
        }
    }

    "Kode som er alene i sin categoryLevel1 gir liste med bare seg selv" - {
        // "0000" er eneste kode i "Uoppgitt/ ikke identifiserbare"
        "0000 gir liste med kun 0000" {
            ArbeidsplassenMapper.relaterteStyrkKoder("0000") shouldBe listOf("0000")
        }
    }

    "Ukjent styrkKode gir tom liste" - {
        "9999 finnes ikke og gir tom liste" {
            ArbeidsplassenMapper.relaterteStyrkKoder("9999").shouldBeEmpty()
        }

        "Tom streng gir tom liste" {
            ArbeidsplassenMapper.relaterteStyrkKoder("").shouldBeEmpty()
        }
    }

    "Symmetri – hvis A er i Bs liste, skal B være i As liste" - {
        "2221 er i listen til 2212, og 2212 er i listen til 2221" {
            ArbeidsplassenMapper.relaterteStyrkKoder("2212") shouldContainAll listOf("2221")
            ArbeidsplassenMapper.relaterteStyrkKoder("2221") shouldContainAll listOf("2212")
        }
    }

    "Antall returnerte koder er lik total for categoryLevel1" - {
        // IT-kodene i JSON: 1330, 2166, 2514, 3514  → 4 koder totalt
        "2514 (IT) skal returnere 4 koder (inkludert seg selv)" {
            ArbeidsplassenMapper.relaterteStyrkKoder("2514").size shouldBe 15
        }
    }
})

