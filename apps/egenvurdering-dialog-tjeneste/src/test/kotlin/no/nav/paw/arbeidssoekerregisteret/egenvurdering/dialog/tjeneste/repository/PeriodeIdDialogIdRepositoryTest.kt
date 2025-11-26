package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.test.buildPostgresDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import java.util.*

class PeriodeIdDialogIdRepositoryTest : FreeSpec({

    val periodeIdDialogIdRepository = PeriodeIdDialogIdRepository

    val dataSource = autoClose(buildPostgresDataSource())
    beforeSpec { Database.connect(dataSource) }

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

    "Update oppdaterer dialogId for eksisterende periodeId" {
        val periodeId = UUID.randomUUID()
        val opprinnelig = 11L
        val ny = 12L

        periodeIdDialogIdRepository.insert(periodeId, opprinnelig)
        periodeIdDialogIdRepository.update(periodeId, ny)

        periodeIdDialogIdRepository.getDialogIdOrNull(periodeId) shouldBe ny
    }

    "Update av ikke-eksisterende periodeId kaster UpdateFeilet" {
        val ukjentPeriode = UUID.randomUUID()
        shouldThrow<UpdateFeilet> {
            periodeIdDialogIdRepository.update(ukjentPeriode, 999L)
        }
    }

    "Update som bryter unikhet (dialogId finnes på annen periode) kaster UpdateFeilet" {
        val periodeId1 = UUID.randomUUID()
        val periodeId2 = UUID.randomUUID()
        val dialogId1 = 200L
        val dialogId2 = 201L

        periodeIdDialogIdRepository.insert(periodeId1, dialogId1)
        periodeIdDialogIdRepository.insert(periodeId2, dialogId2)

        shouldThrow<UpdateFeilet> {
            periodeIdDialogIdRepository.update(periodeId2, dialogId1)
        }

        periodeIdDialogIdRepository.getDialogIdOrNull(periodeId1) shouldBe dialogId1
        periodeIdDialogIdRepository.getDialogIdOrNull(periodeId2) shouldBe dialogId2
    }
})