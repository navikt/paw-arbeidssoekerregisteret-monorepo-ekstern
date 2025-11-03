package no.naw.paw.minestillinger.brukerprofil.flagg

import java.time.Duration
import java.time.Duration.between
import java.time.Instant


sealed interface Flaggtype<A: Flagg> {
    val type: String
    fun flagg(verdi: Boolean, tidspunkt: Instant): Flagg
}

sealed interface Flagg {
    val type: Flaggtype<*>
    val verdi: Boolean
    val tidspunkt: Instant

    fun debug(): String {
        return "Flagg(type=${type.type}, verdi=$verdi, tidspunkt=$tidspunkt)"
    }
}

/**
 * Markerer at flagget lagres direkte i databasen, andre typer flagg er utledet fra annen data.
 */
sealed interface LagretFlagg: Flagg

fun Flagg.erFremdelesGyldig(
    tidspunkt: Instant,
    gydlighetsperiode: Duration
): Boolean = between(this.tidspunkt, tidspunkt) < gydlighetsperiode

fun Flagg.harUtløpt(
    tidspunkt: Instant,
    gydlighetsperiode: Duration
): Boolean = !erFremdelesGyldig(tidspunkt, gydlighetsperiode)
