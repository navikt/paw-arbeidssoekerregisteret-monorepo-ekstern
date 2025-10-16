package no.naw.paw.brukerprofiler.api.vo

import io.kotest.core.spec.style.FreeSpec
import no.naw.paw.brukerprofiler.kodeverk.SSBKodeverk

class ToStyrkTreTest : FreeSpec({

    "Vi kan generere en tre av Styrk koder" {
        val tre = SSBKodeverk.styrkKoder.styrkTre()
        println(tre.joinToString("") { it.prettyPrint("") })
    }

})
