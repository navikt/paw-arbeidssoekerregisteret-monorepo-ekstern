package no.naw.paw.brukerprofiler.kodeverk

data class SSBFylke(
    val code: String,
    val name: String,
    val nameList: List<String> = emptyList()
)

data class SSBKommune(
    val code: String,
    val name: String,
)

data class SSBStyrkKode(
    val code: String,
    val parentCode: String,
    val level: String,
    val name: String
)