package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.aareg.model.Arbeidsforhold
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import kotlin.Comparator

infix fun OpenEndRange<LocalDate>.overlapsWith(other: OpenEndRange<LocalDate>): Boolean =
    start < other.endExclusive && endExclusive > other.start

fun evaluerArbeidsErfaring(
    marginForSammenhengendeJobbDager: Int,
    minimumsArbeidserfaring: Period,
    periode: OpenEndRange<LocalDate>,
    arbeidsforhold: List<Arbeidsforhold>
): Set<ProfileringsTagger> {
    val sammenhengendeJobber = lengsteSammenhengendeInnenforPeriode(
        arbeidsforhold = arbeidsforhold,
        periode = periode,
        marginForSammenhengendeJobbDager = marginForSammenhengendeJobbDager
    )
    return when {
        sammenhengendeJobber == null -> emptySet()
        periodComparator.compare(
            sammenhengendeJobber.periodeInnenfor(periode).length(),
            minimumsArbeidserfaring
        ) >= 0 -> setOf(
            OPPFYLLER_KRAV_TIL_ARBEIDSERFARING
        )
        else -> emptySet()
    }
}

fun lengsteSammenhengendeInnenforPeriode(
    arbeidsforhold: List<Arbeidsforhold>,
    periode: OpenEndRange<LocalDate>,
    marginForSammenhengendeJobbDager: Int
): SammhengendeJobb? =
    arbeidsforhold.flettSammenhengendeJobber(marginForSammenhengendeJobbDager)
        .filter { sammenhengendePeriode ->
            sammenhengendePeriode.tid overlapsWith periode
        }.maxWithOrNull(sammhengendeJobbLengdeComperator(periode))

fun sammhengendeJobbLengdeComperator(innenForPeriode: OpenEndRange<LocalDate>) = Comparator<SammhengendeJobb> { o1, o2 ->
    val length1 = o1.periodeInnenfor(innenForPeriode).length()
    val length2 = o2.periodeInnenfor(innenForPeriode).length()
    periodComparator.compare(length1, length2)
}

val periodComparator = Comparator<Period> { o1, o2 ->
    when {
        o1.years != o2.years -> o1.years.compareTo(o2.years)
        o1.months != o2.months -> o1.months.compareTo(o2.months)
        else -> o1.days.compareTo(o2.days)
    }
}

fun SammhengendeJobb.periodeInnenfor(periode: OpenEndRange<LocalDate>): OpenEndRange<LocalDate> =
    maxOf(tid.start, periode.start).rangeUntil(minOf(tid.endExclusive, periode.endExclusive))

fun OpenEndRange<LocalDate>.length(): Period = Period.between(start, endExclusive)
fun List<Arbeidsforhold>.flettSammenhengendeJobber(marginInDays: Int): List<SammhengendeJobb> =
    if (isEmpty()) {
        emptyList()
    } else {
        drop(1)
            .fold(listOf(somSammenhengendeJobb(first(), marginInDays))) { acc, arbeidsforhold ->
                val match = acc.find { it.erDelAv(arbeidsforhold) }
                if (match != null) {
                    acc - match + match.leggTil(arbeidsforhold)
                } else {
                    acc + somSammenhengendeJobb(arbeidsforhold, marginInDays)
                }
            }
    }

fun somSammenhengendeJobb(arbeidsforhold: Arbeidsforhold, marginInDays: Int): SammhengendeJobb {
    return SammhengendeJobb(
        marginInDays = marginInDays,
        tid = arbeidsforhold.ansettelsesperiode.periode.fom.rangeUntil(
            arbeidsforhold.ansettelsesperiode.periode.tom ?: LocalDate.MAX
        ),
        arbeidsforhold = listOf(arbeidsforhold)
    )
}

data class SammhengendeJobb(
    private val marginInDays: Int,
    val tid: OpenEndRange<LocalDate>,
    val arbeidsforhold: List<Arbeidsforhold>
) {
    private val tidMedMargin
        get() = tid.start.minus(marginInDays.toLong(), ChronoUnit.DAYS).rangeUntil(
            if (tid.endExclusive == LocalDate.MAX) LocalDate.MAX else tid.endExclusive.plus(
                marginInDays.toLong(),
                ChronoUnit.DAYS
            )
        )

    fun erDelAv(arbeidsforhold: Arbeidsforhold): Boolean {
        return tidMedMargin overlapsWith arbeidsforhold.ansettelsesperiode.periode.fom.rangeUntil(
            arbeidsforhold.ansettelsesperiode.periode.tom ?: LocalDate.MAX
        )
    }

    fun leggTil(arbeidsforhold: Arbeidsforhold): SammhengendeJobb {
        if (!erDelAv(arbeidsforhold)) throw IllegalArgumentException("Arbeidsforholdet er ikke del av denne tidslinjen")
        val nyStart = minOf(
            a = tid.start,
            b = arbeidsforhold.ansettelsesperiode.periode.fom
        )
        val nySlutt = maxOf(
            a = tid.endExclusive,
            b = arbeidsforhold.ansettelsesperiode.periode.tom ?: LocalDate.MAX
        )
        return copy(
            tid = nyStart.rangeUntil(nySlutt),
            arbeidsforhold = this.arbeidsforhold + arbeidsforhold
        )
    }
}