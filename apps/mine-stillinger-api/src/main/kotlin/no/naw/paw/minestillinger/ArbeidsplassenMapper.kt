package no.naw.paw.minestillinger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets.UTF_8

private data class StyrkEntry(
    val styrkCode: String,
    val styrkDescription: String,
    val categoryLevel1: String,
    val categoryLevel2: String
)

/**
 * Ved søk etter direktemeldte stillinger skal vi i første omgang kun søke etter stillinger basert på categoryLevel1.
 * Denne klassen mapper en styrkKode til alle andre styrkKoder som har samme categoryLevel1.
 */
object ArbeidsplassenMapper {

    private val styrkTilSammeCategoryLevel1: Map<String, List<String>> = buildStyrkMap()

    fun relaterteStyrkKoder(styrkCode: String): List<String> =
        styrkTilSammeCategoryLevel1[styrkCode] ?: emptyList()

    private fun buildStyrkMap(): Map<String, List<String>> {
        val json = readResourceUtf8("/arbeidsplassen_styrk.json")
        val entries: Map<String, StyrkEntry> = jacksonObjectMapper().readValue(json)

        val byCategoryLevel1: Map<String, List<String>> = entries.values
            .groupBy { it.categoryLevel1 }
            .mapValues { (_, group) -> group.map { it.styrkCode } }

        return entries.keys.associateWith { code ->
            val entry = entries.getValue(code)
            require(entry.styrkCode.equals(code, ignoreCase = true))
            byCategoryLevel1[entry.categoryLevel1] ?: emptyList()
        }
    }

    private fun readResourceUtf8(path: String): String {
        val stream = ArbeidsplassenMapper::class.java.getResourceAsStream(path)
            ?: error("$path ikke funnet. Ligger den i src/main/resources?")
        return InputStreamReader(stream, UTF_8).use { it.readText() }
    }
}
