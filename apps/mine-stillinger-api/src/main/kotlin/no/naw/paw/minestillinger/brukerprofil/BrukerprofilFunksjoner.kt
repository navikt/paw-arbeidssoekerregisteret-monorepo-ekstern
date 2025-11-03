package no.naw.paw.minestillinger.brukerprofil

import no.naw.paw.minestillinger.brukerprofil.TjenestenKanAktiveresResultat.ADRESSE_GRADERING_MÅ_SJEKKES
import no.naw.paw.minestillinger.brukerprofil.TjenestenKanAktiveresResultat.JA
import no.naw.paw.minestillinger.brukerprofil.TjenestenKanAktiveresResultat.NEI
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGradertAdresseFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.erFremdelesGyldig
import no.naw.paw.minestillinger.domain.BrukerProfil
import java.time.Duration
import java.time.Instant

enum class TjenestenKanAktiveresResultat {
    JA,
    NEI,
    ADRESSE_GRADERING_MÅ_SJEKKES
}

fun BrukerProfil.tjenestenKanAktiveres(
    tidspunkt: Instant,
    adresseGraderingGyldighet: Duration
): TjenestenKanAktiveresResultat {
    if (!erITestGruppen) return NEI
    if (!harBruktTjenesten && !harGodeMuligheter) return NEI
    val harGradertAdresse = flagg<HarGradertAdresseFlagg>()
    return when {
        harGradertAdresse == null -> ADRESSE_GRADERING_MÅ_SJEKKES
        harGradertAdresse.erFremdelesGyldig(
            tidspunkt = tidspunkt,
            gydlighetsperiode = adresseGraderingGyldighet
        ) -> if (harGradertAdresse.verdi) NEI else JA
        else -> ADRESSE_GRADERING_MÅ_SJEKKES
    }
}
