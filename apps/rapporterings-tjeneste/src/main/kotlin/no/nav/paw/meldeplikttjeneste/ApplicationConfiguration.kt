package no.nav.paw.meldeplikttjeneste

data class ApplicationConfiguration(
    val periodeTopic: String,
    val ansvarsTopic: String,
    val rapporteringsTopic: String,
    val statStoreName: String
)