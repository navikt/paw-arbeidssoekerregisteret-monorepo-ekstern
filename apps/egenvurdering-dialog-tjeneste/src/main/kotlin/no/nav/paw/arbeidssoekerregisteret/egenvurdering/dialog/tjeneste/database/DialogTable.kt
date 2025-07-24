package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.database

import org.jetbrains.exposed.dao.id.LongIdTable

object DialogTable : LongIdTable("dialog") {
    val dialogId = varchar("dialog_id", 64)
    val egenvurderingId = uuid("egenvurdering_id")
}