package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

object PeriodeIdDialogIdRepository {
    fun getDialogIdOrNull(periodeId: UUID): Long? = transaction {
        PeriodeIdDialogIdTable
            .selectAll()
            .where(PeriodeIdDialogIdTable.periodeId eq periodeId)
            .firstOrNull()
            ?.get(PeriodeIdDialogIdTable.dialogId)
    }

    fun insert(periodeId: UUID, dialogId: Long) {
        return transaction {
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
}

class InsertFeilet(
    periodeId: UUID,
    dialogId: Long,
    cause: Throwable,
) : RuntimeException("Insert feilet for periodeId=$periodeId, dialogId=$dialogId grunnet: ${cause.message}", cause)

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
