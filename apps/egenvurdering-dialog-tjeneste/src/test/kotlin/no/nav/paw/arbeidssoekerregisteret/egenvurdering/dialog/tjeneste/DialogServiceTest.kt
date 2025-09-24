package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste

import io.kotest.core.spec.style.FreeSpec
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.VeilarbdialogClient
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.VeilarbdialogClientConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeIdDialogIdRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_GODE_MULIGHETER
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.client.factory.configureJackson
import no.nav.paw.test.data.periode.BrukerFactory
import no.nav.paw.test.data.periode.MetadataFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import java.time.Instant
import java.util.*

class DialogServiceTest : FreeSpec({

    "Ny tråd opprettes og mapping lagres når ingen dialog finnes fra før" {
        val newDialogId = 555L
        val periodeId = UUID.randomUUID()
        val periodeIdDialogIdRepository = mockk<PeriodeIdDialogIdRepository>()
        every { periodeIdDialogIdRepository.getDialogIdOrNull(periodeId) } returns null
        every { periodeIdDialogIdRepository.insert(periodeId, newDialogId) } just runs

        val engine = MockEngine {
            respond(
                content = """{"id":"$newDialogId"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, Application.Json.toString())
            )
        }
        val client = VeilarbdialogClient(
            config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake"),
            texasClient = mockk(relaxed = true),
            httpClient = testClient(engine)
        )
        val service = DialogService(client, periodeIdDialogIdRepository)

        val egenvurdering = egenvurdering(
            periodeId = periodeId,
            navProfilering = ANTATT_GODE_MULIGHETER,
            brukersEgenvurdering = ANTATT_BEHOV_FOR_VEILEDNING,
            tidspunkt = Instant.parse("2025-03-15T10:00:00Z"),
            fnr = "12345678901"
        )

        val records = consumerRecordsOf(egenvurdering)
        service.varsleVeilederOmEgenvurderingAvProfilering(records)

        verify { periodeIdDialogIdRepository.getDialogIdOrNull(periodeId) }
        verify { periodeIdDialogIdRepository.insert(periodeId, newDialogId) }
        confirmVerified(periodeIdDialogIdRepository)
    }

    "Eksisterende tråd – 200 OK med samme dialogId gir ingen endring i mapping" {
        val existingDialogId = 888L
        val periodeId = UUID.randomUUID()

        val periodeIdDialogIdRepository = mockk<PeriodeIdDialogIdRepository>()
        every { periodeIdDialogIdRepository.getDialogIdOrNull(periodeId) } returns existingDialogId
        every { periodeIdDialogIdRepository.insert(any(), any()) } just runs

        val engine = MockEngine {
            respond(
                content = """{"id":"$existingDialogId"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, Application.Json.toString())
            )
        }
        val client = VeilarbdialogClient(
            config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake"),
            texasClient = mockk(relaxed = true),
            httpClient = testClient(engine)
        )
        val service = DialogService(client, periodeIdDialogIdRepository)

        val egenvurdering = egenvurdering(
            periodeId = periodeId,
            navProfilering = ANTATT_GODE_MULIGHETER,
            brukersEgenvurdering = ANTATT_GODE_MULIGHETER,
            tidspunkt = Instant.parse("2025-03-20T09:00:00Z"),
            fnr = "55555555555"
        )

        val records = consumerRecordsOf(egenvurdering)
        service.varsleVeilederOmEgenvurderingAvProfilering(records)

        verify { periodeIdDialogIdRepository.getDialogIdOrNull(periodeId) }
        verify(exactly = 0) { periodeIdDialogIdRepository.insert(any(), any()) }
        confirmVerified(periodeIdDialogIdRepository)
    }


    "Eksisterende tråd – Oppfølgingsperiode avsluttet gir ingen endring i mapping" {
        val periodeIdDialogIdRepository = mockk<PeriodeIdDialogIdRepository>()
        val existingDialogId = 777L
        val periodeId = UUID.randomUUID()
        every { periodeIdDialogIdRepository.getDialogIdOrNull(periodeId) } returns existingDialogId

        val engine = MockEngine {
            respond(
                content = "Kan ikke sende henvendelse på historisk dialog",
                status = HttpStatusCode.Conflict,
            )
        }
        val client = VeilarbdialogClient(
            config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake"),
            texasClient = mockk(relaxed = true),
            httpClient = testClient(engine)
        )
        val service = DialogService(client, periodeIdDialogIdRepository)

        val egenvurdering = egenvurdering(
            periodeId = periodeId,
            navProfilering = ANTATT_BEHOV_FOR_VEILEDNING,
            brukersEgenvurdering = ANTATT_BEHOV_FOR_VEILEDNING,
            tidspunkt = Instant.parse("2025-03-16T12:00:00Z"),
            fnr = "10987654321"
        )

        val records = consumerRecordsOf(egenvurdering)
        service.varsleVeilederOmEgenvurderingAvProfilering(records)

        verify { periodeIdDialogIdRepository.getDialogIdOrNull(periodeId) }
        confirmVerified(periodeIdDialogIdRepository)
    }

    "DialogId i db er ulik dialogId fra veilarbdialog – Id i db oppdateres" {
        val dialogIdDb = 888L
        val dialogIdFraVeilarb = 999L
        val periodeId = UUID.randomUUID()

        val periodeIdDialogIdRepository = mockk<PeriodeIdDialogIdRepository>()
        every { periodeIdDialogIdRepository.getDialogIdOrNull(periodeId) } returns dialogIdDb
        every { periodeIdDialogIdRepository.update(periodeId, dialogIdFraVeilarb) } just runs

        val engine = MockEngine {
            respond(
                content = """{"id":"$dialogIdFraVeilarb"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, Application.Json.toString())
            )
        }
        val client = VeilarbdialogClient(
            config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake"),
            texasClient = mockk(relaxed = true),
            httpClient = testClient(engine)
        )
        val service = DialogService(client, periodeIdDialogIdRepository)

        val egenvurdering = egenvurdering(
            periodeId = periodeId,
            navProfilering = ANTATT_GODE_MULIGHETER,
            brukersEgenvurdering = ANTATT_GODE_MULIGHETER,
            tidspunkt = Instant.parse("2025-03-20T09:00:00Z"),
            fnr = "55555555555"
        )

        val records = consumerRecordsOf(egenvurdering)
        service.varsleVeilederOmEgenvurderingAvProfilering(records)

        verify { periodeIdDialogIdRepository.getDialogIdOrNull(periodeId) }
        verify(exactly = 0) { periodeIdDialogIdRepository.insert(any(), any()) }
        verify { periodeIdDialogIdRepository.update(periodeId, dialogIdFraVeilarb) }
        confirmVerified(periodeIdDialogIdRepository)
    }
})

private fun consumerRecordsOf(egenvurdering: Egenvurdering): ConsumerRecords<Long, Egenvurdering> {
    val topic = "egenvurdering"
    val record = ConsumerRecord(topic, 0, 0L, 1L, egenvurdering)
    val topicPartition = TopicPartition(topic, 0)
    return ConsumerRecords(mapOf(topicPartition to listOf(record)))
}

private fun egenvurdering(
    periodeId: UUID,
    navProfilering: ProfilertTil,
    brukersEgenvurdering: ProfilertTil,
    tidspunkt: Instant,
    fnr: String,
): Egenvurdering {
    val meta = MetadataFactory.create().build(
        tidspunkt = tidspunkt,
        utfortAv = BrukerFactory.create().build(id = fnr)
    )
    return Egenvurdering(
        UUID.randomUUID(),
        periodeId,
        UUID.randomUUID(),
        meta,
        navProfilering,
        brukersEgenvurdering
    )
}

private fun testClient(engine: MockEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        jackson {
            configureJackson()
        }
    }
}
