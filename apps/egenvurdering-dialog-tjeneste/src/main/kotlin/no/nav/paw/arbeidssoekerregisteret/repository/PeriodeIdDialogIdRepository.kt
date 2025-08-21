package no.nav.paw.arbeidssoekerregisteret.repository

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object PeriodeIdDialogIdRepository {
    fun getDialogIdOrNull(periodeId: UUID): Long? = transaction {
        PeriodeIdDialogIdTable
            .selectAll()
            .where(PeriodeIdDialogIdTable.periodeId eq periodeId)
            .firstOrNull()
            ?.get(PeriodeIdDialogIdTable.dialogId)
    }

    fun insert(periodeId: UUID, dialogId: Long) = transaction {
        try {
            PeriodeIdDialogIdTable.insert {
                it[PeriodeIdDialogIdTable.periodeId] = periodeId
                it[PeriodeIdDialogIdTable.dialogId] = dialogId
            }
        } catch (e: Exception) {
            throw InsertFeilet(periodeId, dialogId, e)
        }
    }
}

class InsertFeilet(
    periodeId: UUID,
    dialogId: Long,
    cause: Throwable
) : RuntimeException(
    "Insert feilet for periodeId=$periodeId, dialogId=$dialogId grunnet: ${cause.message}",
    cause
)