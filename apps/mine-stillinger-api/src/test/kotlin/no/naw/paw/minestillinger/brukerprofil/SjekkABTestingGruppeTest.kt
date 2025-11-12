package no.naw.paw.minestillinger.brukerprofil

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.felles.model.Identitetsnummer


class SjekkABTestingGruppeTest : FreeSpec({
    val matchTestGruppen = "\\d([02468])\\d{9}"
    val ikkeMatchNoen = "[az]{100}"

    "Regex '${matchTestGruppen} skal matche følgende identitetsnummer" - {
        val regex = Regex(matchTestGruppen)
        listOf(
            "12012345678",
            "34098765432",
            "56011122334",
            "78099988877",
            "90055544433",
            "34333333333"
        ).map(::Identitetsnummer).forEach { identitetsnummer ->
            "skal matche ${identitetsnummer.value}, lengde=${identitetsnummer.value.length}" {
                sjekkABTestingGruppe(regex, identitetsnummer) shouldBe true
            }
        }
    }

    "Regex '${matchTestGruppen} skal ikke matche følgende identitetsnummer" - {
        val regex = Regex(matchTestGruppen)
        listOf(
            "11312345678",
            "35198765432",
            "57111122334",
            "79199988877",
            "91155544433",
            "33333333337",
            "23222222222",
            "2a222222222"
        ).map(::Identitetsnummer).forEach { identitetsnummer ->
            "skal ikke matche ${identitetsnummer.value}" {
                sjekkABTestingGruppe(regex, identitetsnummer) shouldBe false
            }
        }
    }
    "Regex '${ikkeMatchNoen} skal ikke matche noen identitetsnummer" - {
        val regex = Regex(ikkeMatchNoen)
        listOf(
            "12012345678",
            "34098765432",
            "56011122334",
            "78099988877",
            "90055544433",
            "34333333333",
            "11312345678",
            "35198765432",
            "57111122334",
            "79199988877",
            "91155544433",
            "33333333337",
            "23222222222",
            "2a222222222"
        ).map(::Identitetsnummer).forEach { identitetsnummer ->
            "skal ikke matche ${identitetsnummer.value}" {
                sjekkABTestingGruppe(regex, identitetsnummer) shouldBe false
            }
        }
    }
})