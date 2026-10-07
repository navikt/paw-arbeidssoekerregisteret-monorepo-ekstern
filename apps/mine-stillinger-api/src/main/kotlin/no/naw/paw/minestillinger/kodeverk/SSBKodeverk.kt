package no.naw.paw.minestillinger.kodeverk

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.MapperFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets.UTF_8
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy

const val UOPPGITT_FYLKESNUMMER = "99"
const val UOPPGITT_KOMMUNENUMMER = "9999"

object SSBKodeverk {

    private val objectMapper = jacksonMapperBuilder {
        disable(KotlinFeature.SingletonSupport)
        disable(KotlinFeature.StrictNullChecks)
    }
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
        .disable(EnumFeature.READ_ENUMS_USING_TO_STRING, EnumFeature.WRITE_ENUMS_USING_TO_STRING)
        .accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
        .build()

    val fylker: List<SSBFylke> = loadFylker()
    val kommuner: List<SSBKommune> = loadKommuner()
    val styrkKoder: List<SSBStyrkKode> = loadStyrkKoder()

    private fun loadFylker(): List<SSBFylke> {
        val root = objectMapper.readTree(readResourceUtf8("/fylke.json"))
        val fylker = objectMapper.readerForListOf(SSBFylke::class.java).readValue<List<SSBFylke>>(root.path("codes"))
        return fylker.filterNot { fylke -> fylke.fylkesnummer == UOPPGITT_FYLKESNUMMER }
    }

    private fun loadKommuner(): List<SSBKommune> {
        val root = objectMapper.readTree(readResourceUtf8("/kommune.json"))
        val kommuner = objectMapper.readerForListOf(SSBKommune::class.java).readValue<List<SSBKommune>>(root.path("codes"))
        return kommuner.filterNot { kommune -> kommune.kommunenummer == UOPPGITT_KOMMUNENUMMER}
    }

    private fun loadStyrkKoder(): List<SSBStyrkKode> {
        val root = objectMapper.readTree(readResourceUtf8("/styrk.json"))
        return objectMapper.readerForListOf(SSBStyrkKode::class.java).readValue(root.path("codes"))
    }

    private fun readResourceUtf8(path: String): String {
        val stream = SSBKodeverk::class.java.getResourceAsStream(path)
            ?: error("Fant ikke ressursen $path. Ligger den i src/main/resources?")
        return InputStreamReader(stream, UTF_8).use { it.readText() }
    }
}
