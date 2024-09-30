package no.nav.paw.arbeidssoekerregisteret.model

enum class BrukerType {
    SLUTTBRUKER,
    VEILEDER
}

data class InnloggetBruker(val type: BrukerType, val ident: String)
