package no.nav.paw.arbeidssoekerregisteret.model

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import java.time.Instant
import java.util.*

data class PeriodeInfo(
    val id: UUID,
    val identitetsnummer: String,
    val arbeidssoekerId: Long,
    val startet: Instant,
    val avsluttet: Instant?
)

fun PeriodeInfo.erAvsluttet(): Boolean = avsluttet != null

fun PeriodeInfo.bleAvsluttetTidligereEnn(tidspunkt: Instant): Boolean =
    avsluttet != null && avsluttet.isBefore(tidspunkt)

fun PeriodeInfo.erInnenfor(tidspunkt: Instant): Boolean = startet.isBefore(tidspunkt)

fun PeriodeInfo.buildEnableToggle(microfrontendId: String): Toggle {
    return Toggle(
        action = ToggleAction.ENABLE,
        ident = identitetsnummer,
        microfrontendId = microfrontendId,
        sensitivitet = Sensitivitet.HIGH,
        initialedBy = "paw" // TODO Styre dette med konfig/miljøvar?
    )
}

fun PeriodeInfo.buildDisableToggle(microfrontendId: String): Toggle {
    return Toggle(
        action = ToggleAction.DISABLE,
        ident = identitetsnummer,
        microfrontendId = microfrontendId,
        initialedBy = "paw" // TODO Styre dette med konfig/miljøvar?
    )
}

fun Periode.buildPeriodeInfo(arbeidssoekerId: Long): PeriodeInfo {
    return PeriodeInfo(
        id = id,
        identitetsnummer = identitetsnummer,
        arbeidssoekerId = arbeidssoekerId,
        startet = startet.tidspunkt,
        avsluttet = avsluttet?.tidspunkt
    )
}
