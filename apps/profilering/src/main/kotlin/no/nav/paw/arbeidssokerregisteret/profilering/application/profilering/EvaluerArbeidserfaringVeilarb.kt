package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.aareg.model.Arbeidsforhold
import java.time.LocalDate
import java.util.*

// Fra Veilarbregistrering
fun veilarbHarJobbetSammenhengendeSeksAvTolvSisteManeder(
    dagensDato: LocalDate,
    arbeidsforhold: List<Arbeidsforhold>,
): Set<ProfileringsTagger> {
    var antallSammenhengendeMaaneder = 0
    val minAntallMndSammenhengendeJobb = 6
    var mndFraDagensMnd = 0
    var innevaerendeMnd = dagensDato.withDayOfMonth(1)

    while (antallSammenhengendeMaaneder < minAntallMndSammenhengendeJobb && mndFraDagensMnd < 12) {
        if (harArbeidsforholdPaaDato(
                innevaerendeMnd,
                arbeidsforhold,
            )
        ) {
            antallSammenhengendeMaaneder++
        } else {
            antallSammenhengendeMaaneder = 0
        }
        innevaerendeMnd = innevaerendeMnd.minusMonths(1)
        mndFraDagensMnd += 1
    }
    return if (antallSammenhengendeMaaneder >= minAntallMndSammenhengendeJobb)
        setOf(
            ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING
        )
    else emptySet()
}

fun harArbeidsforholdPaaDato(
    innevaerendeMnd: LocalDate,
    flereArbeidsforhold: List<Arbeidsforhold>,
): Boolean =
    flereArbeidsforhold.any {
        innevaerendeMnd.isAfter(it.ansettelsesperiode.startdato.minusDays(1)) &&
                (Objects.isNull(it.ansettelsesperiode.sluttdato) || innevaerendeMnd.isBefore(it.ansettelsesperiode.sluttdato!!.plusDays(1)))
    }
