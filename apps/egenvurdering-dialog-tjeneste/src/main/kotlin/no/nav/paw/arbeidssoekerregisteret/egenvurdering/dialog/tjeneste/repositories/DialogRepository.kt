package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repositories

import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.database.DialogTable
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.utils.buildLogger
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class DialogRepository {
    private val logger = buildLogger

    fun findDialogId(
        egenvurderingId: UUID,
    ): DialogRow? =
        transaction {
            DialogTable.selectAll().where { DialogTable.egenvurderingId eq egenvurderingId }
                .map {
                    DialogRow(
                        id = it[DialogTable.id].value,
                        dialogId = it[DialogTable.dialogId],
                        egenvurderingId = it[DialogTable.egenvurderingId]
                    )
                }.firstOrNull()
        }

    fun insertDialogId(egenvurderingId: UUID, dialogId: String) {
        transaction {
            val eksisterendeDialogId = DialogTable.selectAll().where(DialogTable.dialogId eq dialogId).firstOrNull()
            if (eksisterendeDialogId != null) {
                logger.warn("Ignorerer dialogId som duplikat")
            } else {
                logger.info("Lagrer ny dialogId")
                DialogTable.insert {
                    it[DialogTable.dialogId] = dialogId
                    it[DialogTable.egenvurderingId] = egenvurderingId
                }
            }
        }
    }
}

data class DialogRow(
    val id: Long,
    val dialogId: String,
    val egenvurderingId: UUID,
)