package no.naw.paw.minestillinger.jsonkontrakt

import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.NumberFormat
import io.kotest.assertions.json.PropertyOrder
import io.kotest.assertions.json.TypeCoercion
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue

/**
 * Felles hjelpefunksjoner for JSON-kontrakttester (fasitfiler).
 *
 * Fasitfilene under `src/test/resources/json-kontrakt/` er generert fra produksjonskoden
 * og låser dagens JSON-format. Fasitfilene må aldri endres. Et nytt format gir en ny fil;
 * gamle filer for lagrede data skal bli liggende og fortsatt kunne leses.
 */
object JsonKontrakt {
    const val ROT = "json-kontrakt"

    fun lesFasit(sti: String): String {
        val fullSti = "$ROT/$sti"
        return requireNotNull(javaClass.classLoader.getResource(fullSti)) {
            "Fant ikke fasitfil '$fullSti'. Fasitfiler genereres fra produksjonskoden, ikke for hånd."
        }.readText(Charsets.UTF_8)
    }
}

/**
 * Streng sammenligning: rekkefølge på felter og lister, ingen ekstra felter,
 * eksakt tallformat og ingen typekonvertering.
 */
infix fun String.skalVaereLikFasit(sti: String) {
    val fasit = JsonKontrakt.lesFasit(sti)
    withClue("Fasitfil: ${JsonKontrakt.ROT}/$sti") {
        this shouldEqualJson {
            propertyOrder = PropertyOrder.Strict
            fieldComparison = FieldComparison.Strict
            numberFormat = NumberFormat.Strict
            arrayOrder = ArrayOrder.Strict
            typeCoercion = TypeCoercion.Disabled
            fasit
        }
    }
}

/**
 * Som [skalVaereLikFasit], men uten krav til rekkefølge på felter i objekter.
 * Brukes kun når Postgres `jsonb` har normalisert innholdet (jsonb lagrer ikke feltrekkefølge).
 */
infix fun String.skalVaereLikFasitUtenFeltrekkefoelge(sti: String) {
    val fasit = JsonKontrakt.lesFasit(sti)
    withClue("Fasitfil: ${JsonKontrakt.ROT}/$sti (jsonb, feltrekkefølge ignorert)") {
        this shouldEqualJson {
            propertyOrder = PropertyOrder.Lenient
            fieldComparison = FieldComparison.Strict
            numberFormat = NumberFormat.Strict
            arrayOrder = ArrayOrder.Strict
            typeCoercion = TypeCoercion.Disabled
            fasit
        }
    }
}

