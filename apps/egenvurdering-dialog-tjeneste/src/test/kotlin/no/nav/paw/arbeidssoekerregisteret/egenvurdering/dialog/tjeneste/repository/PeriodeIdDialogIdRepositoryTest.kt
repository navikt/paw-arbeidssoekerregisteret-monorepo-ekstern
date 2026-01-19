package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeIdDialogIdTable.getByWithAudit
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.test.buildPostgresDataSource
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insert
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

        getByWithAudit(periodeId).let { row ->
            row.shouldNotBeNull()
            row.periodeId shouldBe periodeId
            row.dialogId shouldBe dialogId
            row.periodeDialogAuditRows.shouldBeEmpty()
            row.finnSisteAuditRow().shouldBeNull()
        }
    }

    "Returnerer null ved ukjent periode" {
        getByWithAudit(UUID.randomUUID()).shouldBeNull()
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

            getByWithAudit(periodeId).let { periodeDialogRow ->
                periodeDialogRow.shouldNotBeNull()
                periodeDialogRow.periodeId shouldBe periodeId
                periodeDialogRow.dialogId shouldBe null
                periodeDialogRow.periodeDialogAuditRows.shouldHaveSize(1)
                periodeDialogRow.finnSisteAuditRow()?.let { auditRow ->
                    auditRow.egenvurderingId shouldBe egenvurderingId
                    auditRow.dialogHttpStatusCode shouldBe HttpStatusCode.InternalServerError.value
                    auditRow.dialogErrorMessage shouldBe errorMessage
                }
            }
        }

        "Feilen ble rettet, og vi får 200 OK fra veilarbdialog med en dialogId" {
            periodeIdDialogIdRepository.insert(periodeId, dialogId, egenvurderingId, HttpStatusCode.OK, null)
            getByWithAudit(periodeId).let { periodeDialogRow ->
                periodeDialogRow.shouldNotBeNull()
                periodeDialogRow.periodeId shouldBe periodeId
                periodeDialogRow.dialogId shouldBe dialogId
                periodeDialogRow.periodeDialogAuditRows.shouldHaveSize(2)
                periodeDialogRow.finnSisteAuditRow()?.let { auditRow ->
                    auditRow.egenvurderingId shouldBe egenvurderingId
                    auditRow.dialogHttpStatusCode shouldBe HttpStatusCode.OK.value
                    auditRow.dialogErrorMessage shouldBe null
                }
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
            getByWithAudit(periodeId).let { periodeDialogRow ->
                periodeDialogRow.shouldNotBeNull()
                periodeDialogRow.periodeId shouldBe periodeId
                periodeDialogRow.dialogId shouldBe dialogId
                periodeDialogRow.periodeDialogAuditRows.shouldHaveSize(3)
                periodeDialogRow.finnSisteAuditRow()?.let { auditRow ->
                    auditRow.egenvurderingId shouldBe egenvurderingId
                    auditRow.dialogHttpStatusCode shouldBe HttpStatusCode.Conflict.value
                    auditRow.dialogErrorMessage shouldBe errorMessage
                }
            }

        }
    }

    "Update oppdaterer dialogId for eksisterende periodeId" {
        val periodeId = UUID.randomUUID()
        val opprinnelig = 11L
        val ny = 12L

        periodeIdDialogIdRepository.insert(periodeId, opprinnelig, UUID.randomUUID(), HttpStatusCode.OK, null)
        periodeIdDialogIdRepository.update(periodeId, ny)

        getByWithAudit(periodeId)?.let { row ->
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

        getByWithAudit(periodeId1)?.let { row ->
            row.dialogId shouldBe dialogId1
        }
        getByWithAudit(periodeId2)?.let { row ->
            row.dialogId shouldBe dialogId2
        }
    }
})
