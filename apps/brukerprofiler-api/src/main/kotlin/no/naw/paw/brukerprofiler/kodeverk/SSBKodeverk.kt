package no.naw.paw.brukerprofiler.kodeverk

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets.UTF_8

const val UOPPGITT_FYLKESNUMMER = "99"
const val UOPPGITT_KOMMUNENUMMER = "9999"

object SSBKodeverk {

    private val objectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    // by lazy så de kun lastes 1 gang
    val fylker: List<SSBFylke> by lazy { loadFylker() }
    val kommuner: List<SSBKommune> by lazy { loadKommuner() }
    val styrkKoder: List<SSBStyrkKode> by lazy { loadStyrkKoder() }

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
