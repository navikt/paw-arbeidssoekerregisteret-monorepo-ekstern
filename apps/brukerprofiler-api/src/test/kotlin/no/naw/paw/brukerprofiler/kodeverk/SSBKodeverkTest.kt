package no.naw.paw.brukerprofiler.kodeverk

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

class SSBKodeverkTest : FreeSpec({
    "Kan laste inn SSB data" {
        shouldNotThrowAny {
            SSBKodeverk.fylker
            SSBKodeverk.kommuner
            SSBKodeverk.styrkKoder
        }
    }

    "Sanity check" {
        SSBKodeverk.fylker.shouldNotBeEmpty()
        SSBKodeverk.kommuner.shouldNotBeEmpty()
        SSBKodeverk.styrkKoder.shouldNotBeEmpty()
    }

    "Lazy loading returnerer samme instans" {
        val a = SSBKodeverk.fylker
        val b = SSBKodeverk.fylker
        (a === b) shouldBe true
    }
})