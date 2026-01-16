package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import java.util.UUID

data class PeriodeDialogRow(
    val periodeId: UUID,
    val dialogId: Long?,
    val egenvurderingId: UUID?,
    val dialogHttpStatusCode: Int?,
    val dialogErrorMessage: String?,
)

data class PeriodeDialogAuditRow(
    val periodeId: UUID,
    val egenvurderingId: UUID,
    val dialogHttpStatusCode: Int?,
    val dialogErrorMessage: String?,
)
