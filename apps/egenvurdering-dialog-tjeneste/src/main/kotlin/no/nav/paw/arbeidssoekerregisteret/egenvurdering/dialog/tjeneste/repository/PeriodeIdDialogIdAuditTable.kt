package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.time.Instant
import java.util.*

object PeriodeIdDialogIdAuditTable : LongIdTable("periode_id_dialog_id_audit") {
    val periodeId = uuid("periode_id").references(PeriodeIdDialogIdTable.periodeId)
    val egenvurderingId = uuid("egenvurdering_id")
    val httpStatusCode = short("http_status_code")
    val errorMessage = text("error_message").nullable()

    @SuppressWarnings("Teknisk kolonne")
    val insertedTimestamp = timestamp("inserted_timestamp").clientDefault { Instant.now() }

    fun findBy(periodeId: UUID): List<PeriodeDialogAuditRow> =
        PeriodeIdDialogIdAuditTable
            .selectAll()
            .where { PeriodeIdDialogIdAuditTable.periodeId eq periodeId }
            .map { row ->
                PeriodeDialogAuditRow(
                    id = row[id].value,
                    periodeId = row[PeriodeIdDialogIdAuditTable.periodeId],
                    egenvurderingId = row[egenvurderingId],
                    dialogHttpStatusCode = row[httpStatusCode].toInt(),
                    dialogErrorMessage = row[errorMessage],
                )
            }

    fun insert(
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
}
