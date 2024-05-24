package no.nav.paw.rapportering.api.domain.request

@JvmInline
value class Identitetsnummer(val verdi: String) {
    override fun toString(): String {
        return "*".repeat(verdi.length)
    }
}

fun String.tilIdentitetsnummer(): Identitetsnummer = Identitetsnummer(this)