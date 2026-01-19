package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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
}

