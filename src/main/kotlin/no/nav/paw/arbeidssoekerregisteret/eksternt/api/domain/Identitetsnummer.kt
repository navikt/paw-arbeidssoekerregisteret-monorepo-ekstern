package no.nav.paw.arbeidssoekerregisteret.eksternt.api.domain

data class Identitetsnummer(val verdi: String) {
    override fun toString(): String {
        return "*".repeat(11)
    }
}

fun String.toIdentitetsnummer(): Identitetsnummer = Identitetsnummer(this)
