package no.naw.paw.brukerprofiler.kodeverk

data class SSBFylke(
    val code: String,
    private val name: String,
) {
    val nameList: List<String> = name.split("-").map { it.trim() }
}

data class SSBKommune(
    val code: String,
    private val name: String,
) {
    // splitter "Trondheim - Tråante", men ikke "Stor-Elvdal"
    val nameList: List<String> = name.split(Regex("\\s*-\\s+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val fylkesnummer: String = code.take(2)
}

data class SSBStyrkKode(
    val code: String,
    val parentCode: String?,
    val level: String,
    val name: String,
)
