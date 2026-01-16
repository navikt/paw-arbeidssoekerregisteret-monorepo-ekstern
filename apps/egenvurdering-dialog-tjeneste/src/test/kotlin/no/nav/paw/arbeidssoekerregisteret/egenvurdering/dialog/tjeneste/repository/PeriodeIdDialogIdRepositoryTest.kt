package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.HttpStatusCode
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.test.buildPostgresDataSource
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

class PeriodeIdDialogIdRepositoryTest : FreeSpec({

    val periodeIdDialogIdRepository = PeriodeIdDialogIdRepository

    val dataSource = autoClose(buildPostgresDataSource())
    beforeSpec { Database.connect(dataSource) }

    "Bugfix - Bakoverkompatibilitet for data før audit tabellen ble introdusert" {
        val periodeId = UUID.randomUUID()
        val dialogId = 1000000L

        transaction {
            PeriodeIdDialogIdTable.insert {
                it[PeriodeIdDialogIdTable.periodeId] = periodeId
                it[PeriodeIdDialogIdTable.dialogId] = dialogId
            }
        }

        periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId).let { row ->
            row.shouldNotBeNull()
            row.periodeId shouldBe periodeId
            row.dialogId shouldBe dialogId
            row.dialogErrorMessage shouldBe null
            row.dialogHttpStatusCode shouldBe null
            row.egenvurderingId shouldBe null
        }
    }

    "Returnerer null ved ukjent periode" {
        periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(UUID.randomUUID()).shouldBeNull()
    }

    "Inserting av lik dialogId med ulik periodeId kaster exception" {
        val dialogId = 777L
        val periodeId1 = UUID.randomUUID()
        val periodeId2 = UUID.randomUUID()

        periodeIdDialogIdRepository.insert(periodeId1, dialogId, UUID.randomUUID(), HttpStatusCode.OK, null)
        shouldThrow<ExposedSQLException> {
            periodeIdDialogIdRepository.insert(periodeId2, dialogId, UUID.randomUUID(), HttpStatusCode.OK, null)
        }
    }

    "Insert/Update scenarier" - {
        val periodeId = UUID.randomUUID()
        val egenvurderingId = UUID.randomUUID()
        val dialogId = 100L

        "Lagre at egenvurderingen feilet mot veilarbdialog" {
            val errorMessage = "Feilmelding fra veilarbdialog"
            periodeIdDialogIdRepository.insert(
                periodeId = periodeId,
                dialogId = null,
                egenvurderingId = egenvurderingId,
                httpStatusCode = HttpStatusCode.InternalServerError,
                errorMessage = errorMessage
            )

            periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId).let { auditInfo ->
                auditInfo.shouldNotBeNull()
                auditInfo.periodeId shouldBe periodeId
                auditInfo.dialogId shouldBe null
                auditInfo.egenvurderingId shouldBe egenvurderingId
                auditInfo.dialogHttpStatusCode!! shouldBe HttpStatusCode.InternalServerError.value
                auditInfo.dialogErrorMessage shouldBe errorMessage
            }
        }

        "Feilen ble rettet, og vi får 200 OK fra veilarbdialog med en dialogId" {
            periodeIdDialogIdRepository.insert(periodeId, dialogId, egenvurderingId, HttpStatusCode.OK, null)
            periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId).let { auditInfo ->
                auditInfo.shouldNotBeNull()
                auditInfo.periodeId shouldBe periodeId
                auditInfo.dialogId shouldBe dialogId
                auditInfo.egenvurderingId shouldBe egenvurderingId
                auditInfo.dialogHttpStatusCode!! shouldBe HttpStatusCode.OK.value
                auditInfo.dialogErrorMessage shouldBe null
            }
        }

        "Lagre at bruker har reservert seg i kontakt og reservasjonsregisteret (KRR)" {
            val errorMessage = "Bruker kan ikke varsles"
            periodeIdDialogIdRepository.insert(
                periodeId = periodeId,
                dialogId = null,
                egenvurderingId = egenvurderingId,
                httpStatusCode = HttpStatusCode.Conflict,
                errorMessage = errorMessage
            )
            periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId).let { auditInfo ->
                auditInfo.shouldNotBeNull()
                auditInfo.periodeId shouldBe periodeId
                auditInfo.dialogId shouldBe dialogId
                auditInfo.egenvurderingId shouldBe egenvurderingId
                auditInfo.dialogHttpStatusCode!! shouldBe HttpStatusCode.Conflict.value
                auditInfo.dialogErrorMessage shouldBe errorMessage
            }

        }

        "Assert riktig antall rader" {
            val antallPeriodeIdDialogIdRader = transaction {
                PeriodeIdDialogIdTable.selectAll().where { PeriodeIdDialogIdTable.periodeId eq periodeId }.count()
            }
            antallPeriodeIdDialogIdRader shouldBe 1
            val antallAuditRader = transaction {
                PeriodeIdDialogIdAuditTable.selectAll().where { PeriodeIdDialogIdAuditTable.periodeId eq periodeId }
                    .count()
            }
            antallAuditRader shouldBe 3
        }
    }

    "Update oppdaterer dialogId for eksisterende periodeId" {
        val periodeId = UUID.randomUUID()
        val opprinnelig = 11L
        val ny = 12L

        periodeIdDialogIdRepository.insert(periodeId, opprinnelig, UUID.randomUUID(), HttpStatusCode.OK, null)
        periodeIdDialogIdRepository.update(periodeId, ny)

        periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId)?.let { row ->
            row.dialogId shouldBe ny
        }
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

        periodeIdDialogIdRepository.insert(periodeId1, dialogId1, UUID.randomUUID(), HttpStatusCode.OK, null)
        periodeIdDialogIdRepository.insert(periodeId2, dialogId2, UUID.randomUUID(), HttpStatusCode.OK, null)

        shouldThrow<UpdateFeilet> {
            periodeIdDialogIdRepository.update(periodeId2, dialogId1)
        }

        periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId1)?.let { row ->
            row.dialogId shouldBe dialogId1
        }
        periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId2)?.let { row ->
            row.dialogId shouldBe dialogId2
        }
    }
})
