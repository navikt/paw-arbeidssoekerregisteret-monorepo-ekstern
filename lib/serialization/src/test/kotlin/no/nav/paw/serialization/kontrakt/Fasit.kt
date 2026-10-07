package no.nav.paw.serialization.kontrakt

import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.NumberFormat
import io.kotest.assertions.json.PropertyOrder
import io.kotest.assertions.json.TypeCoercion
import io.kotest.assertions.json.shouldEqualJson
import java.time.Instant
import java.util.UUID

val FAST_ID: UUID = UUID.fromString("3f2b8c1e-6d4a-4e2b-9a7c-1b2c3d4e5f60")
val FAST_TIDSPUNKT: Instant = Instant.parse("2026-01-15T10:15:30.123456Z")

/** Leser en fasitfil fra `src/test/resources/json-kontrakt/...`. */
fun lesFasit(sti: String): String =
    requireNotNull(Thread.currentThread().contextClassLoader.getResource("json-kontrakt/$sti")) {
        "Fant ikke fasitfil json-kontrakt/$sti"
    }.readText()

/** Streng sammenligning: rekkefølge, feltsett, tallformat og typer må være identiske. */
infix fun String.skalVaereLikFasit(fasit: String) {
    this shouldEqualJson {
        propertyOrder = PropertyOrder.Strict
        fieldComparison = FieldComparison.Strict
        numberFormat = NumberFormat.Strict
        arrayOrder = ArrayOrder.Strict
        typeCoercion = TypeCoercion.Disabled
        fasit
    }
}
