package no.nav.paw.felles.model

@JvmInline
value class AktorId(val value: String) {
    override fun toString(): String {
        return "*".repeat(value.length)
    }
}

fun String.asAktorId(): AktorId = AktorId(this)
