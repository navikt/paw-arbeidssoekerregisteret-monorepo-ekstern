package no.naw.paw.minestillinger.domain

enum class KanTilbysTjenesten {
    JA, NEI, UKJENT
}

fun KanTilbysTjenesten.toDbString(): String = this.name
fun kanTilbysTjenestenFromDbString(value: String): KanTilbysTjenesten = KanTilbysTjenesten.valueOf(value)