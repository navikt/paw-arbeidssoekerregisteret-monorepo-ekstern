package no.naw.paw.minestillinger.domain

import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
import java.time.Instant
import java.util.UUID

data class BrukerProfil(
    val id: Long,
    val identitetsnummer: Identitetsnummer,
    val tjenestenErAktiv: Boolean,
    val kanTilbysTjenesten: KanTilbysTjenesten,
    val kanTilbysTjenestenTimestamp: Instant,
    val harBruktTjenesten: Boolean,
    val erIkkeInteressert: Boolean,
    val arbeidssoekerperiodeId: UUID,
    val arbeidssoekerperiodeAvsluttet: Instant?
)

fun BrukerProfil.api(): ApiBrukerprofil {
    return ApiBrukerprofil(
        identitetsnummer = identitetsnummer.verdi,
        kanTilbysTjenestenLedigeStillinger = when (kanTilbysTjenesten) {
            KanTilbysTjenesten.JA -> true
            KanTilbysTjenesten.NEI -> false
            KanTilbysTjenesten.UKJENT -> throw IllegalStateException("Intern feil: 'kanTilbysTjenesten' er ikke utledet")
        },
        erTjenestenLedigeStillingerAktiv = tjenestenErAktiv,
        erIkkeInteressert = erIkkeInteressert,
        stillingssoek = emptyList()
    )
}
