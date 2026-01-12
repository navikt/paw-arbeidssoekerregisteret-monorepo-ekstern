package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object PeriodeIdDialogIdAuditTable : LongIdTable("periode_id_dialog_id_audit") {
    val periodeId = uuid("periode_id").references(PeriodeIdDialogIdTable.periodeId)
    val egenvurderingId = uuid("egenvurdering_id")
    val httpStatusCode = short("http_status_code")
    val errorMessage = text("error_message").nullable()

    @SuppressWarnings("Teknisk kolonne")
    val insertedTimestamp = timestamp("inserted_timestamp").clientDefault { Instant.now() }
}
