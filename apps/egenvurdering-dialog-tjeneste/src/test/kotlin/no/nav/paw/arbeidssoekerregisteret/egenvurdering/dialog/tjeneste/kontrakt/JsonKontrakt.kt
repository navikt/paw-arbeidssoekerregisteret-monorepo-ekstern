package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.kontrakt

import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.NumberFormat
import io.kotest.assertions.json.PropertyOrder
import io.kotest.assertions.json.TypeCoercion
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Hjelpefunksjoner for JSON-kontrakttester (fasitfiler).
 *
 * Fasitfilene under `src/test/resources/json-kontrakt/` er generert fra produksjonskoden og
 * beskriver JSON-formatet vi sender til andre. Fasitfilene må aldri endres. Feiler en test,
 * er det produksjonskoden som har endret kontrakten.
 */
object JsonKontrakt {

    /** Fast verdi som erstatter tilfeldig `id` i ProblemDetails. */
    const val NOEYTRAL_PROBLEM_ID = "00000000-0000-0000-0000-000000000000"

    /** Fast verdi som erstatter `timestamp` (Instant.now()) i ProblemDetails. */
    const val NOEYTRALT_PROBLEM_TIDSPUNKT = "2026-01-15T10:15:30.123456Z"

    // Brukes kun til å manipulere JSON-treet, aldri til å serialisere domeneobjekter.
    private val treMapper = JsonMapper()

    fun lesFasit(sti: String): String =
        JsonKontrakt::class.java.getResource("/json-kontrakt/$sti")?.readText()
            ?: error("Fant ikke fasitfil json-kontrakt/$sti")

    infix fun String.skalVaereLikFasit(sti: String) {
        this shouldEqualJson {
            propertyOrder = PropertyOrder.Strict
            fieldComparison = FieldComparison.Strict
            numberFormat = NumberFormat.Strict
            arrayOrder = ArrayOrder.Strict
            typeCoercion = TypeCoercion.Disabled
            lesFasit(sti)
        }
    }

    /**
     * Erstatter `id` og `timestamp` i en ProblemDetails-body med faste verdier, slik at bodyen kan
     * sammenlignes med fasit. Sjekker først at feltene finnes og er tekst (ISO-8601/UUID), slik at
     * en endring i typen fortsatt fanges opp. Rekkefølgen på feltene beholdes.
     */
    fun noeytraliserProblemDetails(json: String): String {
        val node = treMapper.readTree(json) as ObjectNode
        node.get("id") shouldNotBe null
        node.get("id").isTextual shouldBe true
        node.get("timestamp") shouldNotBe null
        node.get("timestamp").isTextual shouldBe true
        node.put("id", NOEYTRAL_PROBLEM_ID)
        node.put("timestamp", NOEYTRALT_PROBLEM_TIDSPUNKT)
        return treMapper.writeValueAsString(node)
    }
}
