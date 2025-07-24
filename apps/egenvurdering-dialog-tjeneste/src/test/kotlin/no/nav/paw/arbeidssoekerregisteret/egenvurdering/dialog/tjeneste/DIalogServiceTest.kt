package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste

import io.kotest.core.spec.style.FreeSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repositories.DialogRepository
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repositories.DialogRow
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.services.DialogService
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as EgenvurderingMetadata
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class DialogServiceTest : FreeSpec({
    "DialogService" - {
        val veilarbdialogClient = mockk<VeilarbdialogClient>()
        val dialogRepository = mockk<DialogRepository>()
        val dialogService = DialogService(veilarbdialogClient, dialogRepository)

        "should create a new dialog if no existing dialogId is found" {
            val egenvurdering = Egenvurdering(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                EgenvurderingMetadata(
                    Instant.now(),
                    Bruker(
                        BrukerType.SLUTTBRUKER,
                        "12345678910",
                        "test-sikkerhetsnivaa"
                    ),
                    "test-kilde",
                    "test-aarsak",
                    null,
                ),
                ProfilertTil.ANTATT_GODE_MULIGHETER,
                ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
            )
            val dialogRequest = DialogRequest(
                tekst = "Nav sin vurdering: Vi tror du har gode muligheter til å komme i jobb uten en veileder eller tiltak fra Nav.\n\nMin vurdering: Jeg trenger en veileder for å komme i arbeid. \n\nDette er en automatisk generert melding",
                dialogId = null,
                overskrift = "Egenvurdering ${LocalDate.now()}",
                venterPaaSvarFraNav = true,
                venterPaaSvarFraBruker = false,
                fnr = "12345678910"
            )
            val dialogResponse = DialogResponse(dialogId = "new-dialog-id")

            coEvery { dialogRepository.findDialogId(egenvurdering.id) } returns null
            coEvery { veilarbdialogClient.lagEllerOppdaterDialog(dialogRequest) } returns dialogResponse
            coEvery { dialogRepository.insertDialogId(egenvurdering.id, "new-dialog-id") } returns Unit

            val records = ConsumerRecords<Long, Egenvurdering>(
                mapOf(
                    TopicPartition("egenvurdering-topic", 0) to listOf(
                        ConsumerRecord("egenvurdering-topic", 0, 0L, 0L, egenvurdering)
                    )
                )
            )

            runBlocking {
                dialogService.handleRecords(records)
            }

            coVerify { dialogRepository.findDialogId(egenvurdering.id) }
            coVerify { veilarbdialogClient.lagEllerOppdaterDialog(dialogRequest) }
            coVerify { dialogRepository.insertDialogId(egenvurdering.id, "new-dialog-id") }
        }

        "should update an existing dialog if dialogId is found" {
            val egenvurdering = Egenvurdering(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                EgenvurderingMetadata(
                    Instant.now(),
                    Bruker(
                        BrukerType.SLUTTBRUKER,
                        "12345678910",
                        "test-sikkerhetsnivaa"
                    ),
                    "test-kilde",
                    "test-aarsak",
                    null,
                ),
                ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING,
                ProfilertTil.ANTATT_GODE_MULIGHETER,
            )
            val existingDialogId = "existing-dialog-id"
            val dialogRequest = DialogRequest(
                tekst = "Nav sin vurdering: Vi tror du vil trenge hjelp fra en veileder for å nå ditt mål om arbeid.\n\nMin vurdering: Nei, jeg vil gjerne klare meg selv. \n\nDette er en automatisk generert melding",
                dialogId = existingDialogId,
                overskrift = "Egenvurdering ${LocalDate.now()}",
                venterPaaSvarFraNav = false,
                venterPaaSvarFraBruker = false,
                fnr = "12345678910"
            )

            val dialogResponse = DialogResponse(dialogId = existingDialogId)

            coEvery { dialogRepository.findDialogId(egenvurdering.id) } returns DialogRow(
                1,
                existingDialogId,
                egenvurdering.id
            )
            coEvery { veilarbdialogClient.lagEllerOppdaterDialog(dialogRequest) } returns dialogResponse

            val records = ConsumerRecords<Long, Egenvurdering>(
                mapOf(
                    TopicPartition("egenvurdering-topic", 0) to listOf(
                        ConsumerRecord("egenvurdering-topic", 0, 0L, 0L, egenvurdering)
                    )
                )
            )

            runBlocking {
                dialogService.handleRecords(records)
            }

            coVerify { dialogRepository.findDialogId(egenvurdering.id) }
            coVerify { veilarbdialogClient.lagEllerOppdaterDialog(dialogRequest) }
        }
    }
})