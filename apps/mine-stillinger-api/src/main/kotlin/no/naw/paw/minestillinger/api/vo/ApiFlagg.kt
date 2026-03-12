package no.naw.paw.minestillinger.api.vo

import no.naw.paw.minestillinger.brukerprofil.flagg.Flagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBeskyttetadresseFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBruktTjenestenFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGodeMuligheterFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.InkluderDirekteMeldteStillingerFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.OptOutFlag
import no.naw.paw.minestillinger.brukerprofil.flagg.StandardInnsatsFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlagg
import java.time.Instant

data class ApiFlagg(
    val navn: ApiFlaggNavn,
    val tidspunkt: Instant?
)

enum class ApiFlaggNavn {
    OPT_OUT,
    DIREKTEMELDTE_STILLINGER,
    TJENESTEN_AKTIVERT
}

fun Flagg.api(): ApiFlagg? {
    val tidspunkt = this.tidspunkt
    return when (this) {
        is HarGodeMuligheterFlagg -> null
        is HarBeskyttetadresseFlagg -> null
        is HarBruktTjenestenFlagg -> null
        is InkluderDirekteMeldteStillingerFlagg -> {
            if (this.verdi) {
                ApiFlagg(
                    navn = ApiFlaggNavn.DIREKTEMELDTE_STILLINGER,
                    tidspunkt = tidspunkt
                )
            } else null
        }
        is OptOutFlag -> {
            if (this.verdi) {
                ApiFlagg(
                    navn = ApiFlaggNavn.OPT_OUT,
                    tidspunkt = tidspunkt
                )
            } else null
        }
        is StandardInnsatsFlagg -> null
        is TjenestenErAktivFlagg -> {
            if (this.verdi) {
                ApiFlagg(
                    navn = ApiFlaggNavn.TJENESTEN_AKTIVERT,
                    tidspunkt = tidspunkt
                )
            } else null
        }
    }
}