package no.nav.paw.arbeidssoekerregisteret.model

import java.time.Instant
import java.util.*

data class PeriodeInfo(
    val id: UUID,
    val identitetsnummer: String,
    val arbeidssoekerId: Long,
    val startet: Instant,
    val avsluttet: Instant?
) {
    fun erAktiv(): Boolean = avsluttet == null

    fun erAvsluttet(): Boolean = avsluttet != null

    fun bleAvsluttetTidligereEnn(tidspunkt: Instant): Boolean =
        avsluttet != null && avsluttet.isBefore(tidspunkt)

    fun erStartetEtter(tidspunkt: Instant): Boolean = startet.isAfter(tidspunkt)

    fun asEnableToggle(
        microfrontend: MicroFrontend,
        sensitivitet: Sensitivitet,
        initiatedBy: String = "paw" // TODO Styre dette med konfig/miljøvar?
    ): Toggle {
        return Toggle(
            action = ToggleAction.ENABLE,
            ident = identitetsnummer,
            microfrontendId = microfrontend,
            sensitivitet = sensitivitet,
            initiatedBy = initiatedBy
        )
    }

    fun asDisableToggle(
        microfrontend: MicroFrontend,
        initiatedBy: String = "paw" // TODO Styre dette med konfig/miljøvar?
    ): Toggle {
        return Toggle(
            action = ToggleAction.DISABLE,
            ident = identitetsnummer,
            microfrontendId = microfrontend,
            initiatedBy = initiatedBy
        )
    }
}