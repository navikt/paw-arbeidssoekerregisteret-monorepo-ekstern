package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

object PeriodeIdDialogIdTable : Table("periode_id_dialog_id") {
    val periodeId = uuid("periode_id")
    val dialogId = long("dialog_id")
    @SuppressWarnings("Teknisk kolonne")
    val insertedTimestamp = timestamp("inserted_timestamp").clientDefault { Instant.now() }

    override val primaryKey = PrimaryKey(periodeId)
    init {
        uniqueIndex("uk_dialog_id", dialogId)
    }
}