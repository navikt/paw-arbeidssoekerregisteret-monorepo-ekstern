package no.naw.paw.minestillinger.domain

import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
import no.naw.paw.minestillinger.api.vo.ApiTjenesteStatus
import java.time.Instant
import java.util.UUID

data class BrukerProfil(
    val id: Long,
    val identitetsnummer: Identitetsnummer,
    val kanTilbysTjenesten: KanTilbysTjenesten,
    val kanTilbysTjenestenTimestamp: Instant,
    val tjenestestatus: TjenesteStatus,
    val harBruktTjenesten: Boolean,
    val arbeidssoekerperiodeId: UUID,
    val arbeidssoekerperiodeAvsluttet: Instant?
)

fun BrukerProfil.api(): ApiBrukerprofil {
    return ApiBrukerprofil(
        identitetsnummer = identitetsnummer.verdi,
        tjenestestatus = when (tjenestestatus) {
            TjenesteStatus.AKTIV -> ApiTjenesteStatus.AKTIV
            TjenesteStatus.INAKTIV -> ApiTjenesteStatus.INAKTIV
            TjenesteStatus.OPT_OUT -> ApiTjenesteStatus.OPT_OUT
            TjenesteStatus.KAN_IKKE_LEVERES -> ApiTjenesteStatus.KAN_IKKE_LEVERES
        },
        stillingssoek = emptyList()
    )
}
