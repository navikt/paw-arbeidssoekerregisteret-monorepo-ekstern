package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.util.*

object PeriodeIdDialogIdRepository {
    fun insert(
        periodeId: UUID,
        dialogId: Long?,
        egenvurderingId: UUID,
        httpStatusCode: HttpStatusCode,
        errorMessage: String? = null,
    ): Unit = transaction {

        PeriodeIdDialogIdTable.insertIgnore {
            it[PeriodeIdDialogIdTable.periodeId] = periodeId
        }

        if (dialogId != null) {
            PeriodeIdDialogIdTable.update({
                (PeriodeIdDialogIdTable.periodeId eq periodeId) and
                        (PeriodeIdDialogIdTable.dialogId.isNull())
            }) {
                it[PeriodeIdDialogIdTable.dialogId] = dialogId
            }
        }

        insertAuditRow(periodeId, egenvurderingId, httpStatusCode, errorMessage)
    }

    private fun insertAuditRow(
        periodeId: UUID,
        egenvurderingId: UUID,
        httpStatusCode: HttpStatusCode,
        errorMessage: String?,
    ) {
        PeriodeIdDialogIdAuditTable.insert {
            it[PeriodeIdDialogIdAuditTable.periodeId] = periodeId
            it[PeriodeIdDialogIdAuditTable.egenvurderingId] = egenvurderingId
            it[PeriodeIdDialogIdAuditTable.httpStatusCode] = httpStatusCode.value.toShort()
            it[PeriodeIdDialogIdAuditTable.errorMessage] = errorMessage
        }
    }

    fun update(periodeId: UUID, dialogId: Long) = transaction {
        try {
            val rowsUpdated = PeriodeIdDialogIdTable.update(
                where = { PeriodeIdDialogIdTable.periodeId eq periodeId }
            ) {
                it[PeriodeIdDialogIdTable.dialogId] = dialogId
            }
            if (rowsUpdated == 0) throw UpdateFeilet(periodeId, dialogId)
        } catch (uf: UpdateFeilet) {
            throw uf
        } catch (e: Exception) {
            throw UpdateFeilet(periodeId, dialogId, e)
        }
    }

    fun hentPeriodeIdDialogIdInfo(periodeId: UUID): PeriodeDialogRow? = transaction {
        PeriodeIdDialogIdTable
            .join(
                otherTable = PeriodeIdDialogIdAuditTable,
                joinType = JoinType.LEFT,
                onColumn = PeriodeIdDialogIdTable.periodeId,
                otherColumn = PeriodeIdDialogIdAuditTable.periodeId
            )
            .selectAll()
            .where { PeriodeIdDialogIdTable.periodeId eq periodeId }
            .orderBy(PeriodeIdDialogIdAuditTable.id to SortOrder.DESC)
            .firstOrNull()
            ?.let { row ->
                PeriodeDialogRow(
                    periodeId = row[PeriodeIdDialogIdTable.periodeId],
                    dialogId = row.getOrNull(PeriodeIdDialogIdTable.dialogId),
                    egenvurderingId = row.getOrNull(PeriodeIdDialogIdAuditTable.egenvurderingId),
                    dialogHttpStatusCode = row.getOrNull(PeriodeIdDialogIdAuditTable.httpStatusCode)?.toInt(),
                    dialogErrorMessage = row.getOrNull(PeriodeIdDialogIdAuditTable.errorMessage),
                )
            }
    }
}

class UpdateFeilet(
    periodeId: UUID,
    dialogId: Long,
    cause: Throwable? = null,
) : RuntimeException(
    when (cause) {
        null -> "Update feilet for periodeId=$periodeId, dialogId=$dialogId (ingen rad oppdatert)"
        else -> "Update feilet for periodeId=$periodeId, dialogId=$dialogId grunnet: ${cause.message}"
    },
    cause
)
