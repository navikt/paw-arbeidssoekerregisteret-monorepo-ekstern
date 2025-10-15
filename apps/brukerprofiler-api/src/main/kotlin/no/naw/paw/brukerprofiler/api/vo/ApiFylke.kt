package no.naw.paw.brukerprofiler.api.vo

data class ApiFylke(
    val navn: String,
    val kommuner: List<String> = emptyList(),
)