package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repositories.DialogRepository
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repositories.DialogRow
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.utils.initTestDatabase
import org.jetbrains.exposed.sql.Database
import java.util.UUID
import javax.sql.DataSource

class DialogRepositoryTest : FreeSpec({
    lateinit var dataSource: DataSource
    lateinit var repository: DialogRepository

    beforeSpec {
        dataSource = initTestDatabase()
        Database.Companion.connect(dataSource)
        repository = DialogRepository()
    }

    afterSpec {
        dataSource.connection.close()
    }


    "should insert and retrieve dialogId" {
        val egenvurderingId = UUID.randomUUID()
        val dialogId = "test-dialog-id"

        repository.insertDialogId(egenvurderingId, dialogId)

        val result = repository.findDialogId(egenvurderingId)
        result shouldBe DialogRow(1, dialogId, egenvurderingId)
    }

    "should return null if dialogId does not exist" {
        val result = repository.findDialogId(UUID.randomUUID())
        result shouldBe null
    }
})