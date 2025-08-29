package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.jetbrains.exposed.sql.Database
import java.util.*
import javax.sql.DataSource

class PeriodeIdDialogIdRepositoryTest : FreeSpec({
    lateinit var dataSource: DataSource
    lateinit var periodeIdDialogIdRepository: PeriodeIdDialogIdRepository

    beforeSpec {
        dataSource = initTestDatabase()
        Database.connect(dataSource)
        periodeIdDialogIdRepository = PeriodeIdDialogIdRepository
    }
    afterSpec { dataSource.connection.close() }


    "getDialogIdOrNull er null når periode ikke finnes" {
        periodeIdDialogIdRepository.getDialogIdOrNull(UUID.randomUUID()) shouldBe null
    }

    "getDialogIdOrNull returnerer riktig dialogId" {
        val periodeId = UUID.randomUUID()
        val dialogId = 42L

        periodeIdDialogIdRepository.insert(periodeId, dialogId)
        periodeIdDialogIdRepository.getDialogIdOrNull(periodeId) shouldBe dialogId
    }

    "Insert av lik periodeId kaster InsertFeilet" {
        val periodeId = UUID.randomUUID()

        periodeIdDialogIdRepository.insert(periodeId, 100L)

        shouldThrow<InsertFeilet> {
            periodeIdDialogIdRepository.insert(periodeId, 101L)
        }
    }

    "Inserting av lik dialogId med ulik periodeId kaster InsertFeilet" {
        val dialogId = 777L
        val periodeId1 = UUID.randomUUID()
        val periodeId2 = UUID.randomUUID()

        periodeIdDialogIdRepository.insert(periodeId1, dialogId)

        shouldThrow<InsertFeilet> {
            periodeIdDialogIdRepository.insert(periodeId2, dialogId)
        }
    }
})