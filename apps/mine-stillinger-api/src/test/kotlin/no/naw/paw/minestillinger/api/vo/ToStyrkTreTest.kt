package no.naw.paw.minestillinger.api.vo

import io.kotest.core.spec.style.FreeSpec
import no.naw.paw.minestillinger.kodeverk.SSBKodeverk

class ToStyrkTreTest : FreeSpec({

    "Vi kan generere en tre av Styrk koder" {
        val tre = SSBKodeverk.styrkKoder.styrkTre()
        println(tre.joinToString("") { it.prettyPrint("") })
    }

})
