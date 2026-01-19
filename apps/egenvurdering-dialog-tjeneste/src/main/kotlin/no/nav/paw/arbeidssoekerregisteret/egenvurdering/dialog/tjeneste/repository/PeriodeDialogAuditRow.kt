package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import java.util.UUID

data class PeriodeDialogAuditRow(
    val id: Long,
    val periodeId: UUID,
    val egenvurderingId: UUID,
    val dialogHttpStatusCode: Int,
    val dialogErrorMessage: String?,
)
