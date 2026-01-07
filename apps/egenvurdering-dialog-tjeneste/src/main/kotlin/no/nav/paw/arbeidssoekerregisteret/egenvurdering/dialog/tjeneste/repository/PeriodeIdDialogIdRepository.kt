package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
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

    fun setDialogResponseInfo(
        periodeId: UUID,
        httpStatusCode: Int,
        errorMessage: String,
    ) = transaction {
        PeriodeIdDialogIdTable.insertIgnore {
            it[PeriodeIdDialogIdTable.periodeId] = periodeId
            it[PeriodeIdDialogIdTable.dialogHttpStatusCode] = httpStatusCode
            it[PeriodeIdDialogIdTable.dialogErrorMessage] = errorMessage
        }

        PeriodeIdDialogIdTable.update({ PeriodeIdDialogIdTable.periodeId eq periodeId }) {
            it[PeriodeIdDialogIdTable.dialogHttpStatusCode] = httpStatusCode
            it[PeriodeIdDialogIdTable.dialogErrorMessage] = errorMessage
        }
    }

    fun hentDialogInfoFra(periodeId: UUID): PeriodeDialogRow? = transaction {
        PeriodeIdDialogIdTable
            .selectAll()
            .where(PeriodeIdDialogIdTable.periodeId eq periodeId)
            .firstOrNull()
            ?.let { row ->
                PeriodeDialogRow(
                    periodeId = row[PeriodeIdDialogIdTable.periodeId],
                    dialogId = row[PeriodeIdDialogIdTable.dialogId],
                    dialogHttpStatusCode = row[PeriodeIdDialogIdTable.dialogHttpStatusCode],
                    dialogErrorMessage = row[PeriodeIdDialogIdTable.dialogErrorMessage],
                )
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
