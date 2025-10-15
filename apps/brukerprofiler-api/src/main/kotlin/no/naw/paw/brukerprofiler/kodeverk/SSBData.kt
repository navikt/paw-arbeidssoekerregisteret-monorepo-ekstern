package no.naw.paw.brukerprofiler.kodeverk

import com.fasterxml.jackson.annotation.JsonProperty

data class SSBFylke(
    @param:JsonProperty("code")
    val fylkesnummer: String,
    private val name: String,
) {
    val nameList: List<String> = name.split("-").map { it.trim() }
}

data class SSBKommune(
    @param:JsonProperty("code")
    val kommunenummer: String,
    private val name: String,
) {
    // splitter "Trondheim - Tråante", men ikke "Stor-Elvdal"
    val nameList: List<String> = name.split(Regex("\\s*-\\s+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val fylkesnummer: String = kommunenummer.take(2)
}

data class SSBStyrkKode(
    val code: String,
    val parentCode: String?,
    val level: String,
    val name: String,
)
