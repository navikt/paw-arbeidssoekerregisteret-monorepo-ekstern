package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import java.util.*

data class PeriodeDialogRow(
    val periodeId: UUID,
    val dialogId: Long?,
    val periodeDialogAuditRows: List<PeriodeDialogAuditRow>,
) {
    fun finnSisteAuditRow(): PeriodeDialogAuditRow? =
        periodeDialogAuditRows.sortedBy { auditRow -> auditRow.id }.singleOrNull()
}

data class PeriodeDialogAuditRow(
    val id: Long,
    val periodeId: UUID,
    val egenvurderingId: UUID,
    val dialogHttpStatusCode: Int,
    val dialogErrorMessage: String?,
)
