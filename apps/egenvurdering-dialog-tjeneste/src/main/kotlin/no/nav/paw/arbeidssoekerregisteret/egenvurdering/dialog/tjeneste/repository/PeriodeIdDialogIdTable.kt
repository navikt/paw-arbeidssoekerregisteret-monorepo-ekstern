package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Instant
import java.util.*

object PeriodeIdDialogIdTable : Table("periode_id_dialog_id") {
    val periodeId = uuid("periode_id")
    val dialogId = long("dialog_id").nullable()

    @SuppressWarnings("Teknisk kolonne")
    val insertedTimestamp = timestamp("inserted_timestamp").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(periodeId)

    init {
        uniqueIndex("uk_dialog_id", dialogId)
    }

    fun getBy(periodeId: UUID): PeriodeDialogRow? = transaction {
        PeriodeIdDialogIdTable
            .selectAll()
            .where(PeriodeIdDialogIdTable.periodeId eq periodeId)
            .singleOrNull()
            ?.let { row ->
                PeriodeDialogRow(
                    periodeId = row[PeriodeIdDialogIdTable.periodeId],
                    dialogId = row[dialogId],
                    periodeDialogAuditRows = emptyList()
                )
            }
    }

    fun getByWithAudit(periodeId: UUID): PeriodeDialogRow? = transaction {
        getBy(periodeId)?.copy(periodeDialogAuditRows = PeriodeIdDialogIdAuditTable.findBy(periodeId))
    }

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

        PeriodeIdDialogIdAuditTable.insert(periodeId, egenvurderingId, httpStatusCode, errorMessage)
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
}

