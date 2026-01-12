package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.service

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
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.APPLICATION_CONFIG
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.VeilarbdialogClientConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeDialogRow
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeIdDialogIdRepository
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_GODE_MULIGHETER
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import no.nav.paw.client.factory.configureJackson
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.test.data.periode.BrukerFactory
import no.nav.paw.test.data.periode.MetadataFactory
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import java.time.Instant
import java.util.*

class DialogServiceTest : FreeSpec({
    val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG)

    "Ny tråd opprettes og mapping lagres når ingen dialog finnes fra før" {
        val newDialogId = 555L
        val periodeId = UUID.randomUUID()
        val egenvurderingId = UUID.randomUUID()
        val periodeIdDialogIdRepository = mockk<PeriodeIdDialogIdRepository>()
        every { periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId) } returns null
        every {
            periodeIdDialogIdRepository.insert(
                periodeId,
                newDialogId,
                egenvurderingId,
                HttpStatusCode.OK,
                null
            )
        } just runs

        val engine = MockEngine {
            respond(
                content = """{"id":"$newDialogId"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, Application.Json.toString())
            )
        }
        val veilarbdialogClient = VeilarbdialogClient(
            config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake", target = "veilarbdialog.fake"),
            texasClient = mockk(relaxed = true),
            httpClient = testClient(engine)
        )
        val service = DialogService(
            applicationConfig = applicationConfig,
            veilarbdialogClient = veilarbdialogClient,
            periodeIdDialogIdRepository = periodeIdDialogIdRepository
        )

        val egenvurdering = egenvurdering(
            periodeId = periodeId,
            navProfilering = ANTATT_GODE_MULIGHETER,
            egenvurderingId = egenvurderingId,
            brukersEgenvurdering = ANTATT_BEHOV_FOR_VEILEDNING,
            tidspunkt = Instant.parse("2025-03-15T10:00:00Z"),
            fnr = "12345678901"
        )

        val records = consumerRecordsOf(egenvurdering)
        service.varsleVeilederOmEgenvurderingAvProfilering(records)

        verify { periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId) }
        verify { periodeIdDialogIdRepository.insert(periodeId, newDialogId, egenvurderingId, HttpStatusCode.OK, null) }
        confirmVerified(periodeIdDialogIdRepository)
    }

    "Eksisterende tråd – 200 OK med samme dialogId gir ingen endring i mapping" {
        val existingDialogId = 888L
        val periodeId = UUID.randomUUID()

        val periodeIdDialogIdRepository = mockk<PeriodeIdDialogIdRepository>()
        every { periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId) } returns PeriodeDialogRow(
            periodeId = periodeId,
            dialogId = existingDialogId,
            egenvurderingId = UUID.randomUUID(),
            dialogHttpStatusCode = HttpStatusCode.OK.value,
            dialogErrorMessage = null
        )
        every { periodeIdDialogIdRepository.insert(any(), any(), any(), any(), any()) } just runs

        val engine = MockEngine {
            respond(
                content = """{"id":"$existingDialogId"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, Application.Json.toString())
            )
        }
        val veilarbdialogClient = VeilarbdialogClient(
            config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake", target = "veilarbdialog.fake"),
            texasClient = mockk(relaxed = true),
            httpClient = testClient(engine)
        )
        val service = DialogService(
            applicationConfig = applicationConfig,
            veilarbdialogClient = veilarbdialogClient,
            periodeIdDialogIdRepository = periodeIdDialogIdRepository
        )

        val egenvurdering = egenvurdering(
            periodeId = periodeId,
            navProfilering = ANTATT_GODE_MULIGHETER,
            brukersEgenvurdering = ANTATT_GODE_MULIGHETER,
            tidspunkt = Instant.parse("2025-03-20T09:00:00Z"),
            fnr = "55555555555"
        )

        val records = consumerRecordsOf(egenvurdering)
        service.varsleVeilederOmEgenvurderingAvProfilering(records)

        verify { periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId) }
        verify(exactly = 0) { periodeIdDialogIdRepository.insert(any(), any(), any(), any(), any()) }
        confirmVerified(periodeIdDialogIdRepository)
    }


    "Eksisterende tråd – Oppfølgingsperiode avsluttet setter riktig statuskode og feilmelding fra veilarbdialog" {
        val periodeIdDialogIdRepository = mockk<PeriodeIdDialogIdRepository>()
        val existingDialogId = 777L
        val periodeId = UUID.randomUUID()
        val egenvurderingId = UUID.randomUUID()

        every { periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId) } returns PeriodeDialogRow(
            periodeId = periodeId,
            dialogId = existingDialogId,
            egenvurderingId = UUID.randomUUID(),
            dialogHttpStatusCode = HttpStatusCode.OK.value,
            dialogErrorMessage = null
        )

        val errorMessage = "Kan ikke sende henvendelse på historisk dialog"
        every {
            periodeIdDialogIdRepository.insert(
                periodeId = periodeId,
                dialogId = null,
                egenvurderingId = egenvurderingId,
                httpStatusCode = HttpStatusCode.Conflict,
                errorMessage = errorMessage,
            )
        } returns Unit

        val engine = MockEngine {
            respond(
                content = errorMessage,
                status = HttpStatusCode.Conflict,
            )
        }
        val veilarbdialogClient = VeilarbdialogClient(
            config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake", target = "veilarbdialog.fake"),
            texasClient = mockk(relaxed = true),
            httpClient = testClient(engine)
        )
        val service = DialogService(
            applicationConfig = applicationConfig,
            veilarbdialogClient = veilarbdialogClient,
            periodeIdDialogIdRepository = periodeIdDialogIdRepository
        )

        val egenvurdering = egenvurdering(
            periodeId = periodeId,
            egenvurderingId = egenvurderingId,
            navProfilering = ANTATT_BEHOV_FOR_VEILEDNING,
            brukersEgenvurdering = ANTATT_BEHOV_FOR_VEILEDNING,
            tidspunkt = Instant.parse("2025-03-16T12:00:00Z"),
            fnr = "10987654321"
        )

        val records = consumerRecordsOf(egenvurdering)
        service.varsleVeilederOmEgenvurderingAvProfilering(records)

        verify(exactly = 1) { periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId) }
        verify(exactly = 1) {
            periodeIdDialogIdRepository.insert(
                periodeId = periodeId,
                dialogId = null,
                egenvurderingId = egenvurderingId,
                httpStatusCode = HttpStatusCode.Conflict,
                errorMessage = errorMessage,
            )
        }

        confirmVerified(periodeIdDialogIdRepository)
    }

    "Person har reservert seg i kontakt og reservasjonsregisteret (KRR)" {
        val periodeIdDialogIdRepository = mockk<PeriodeIdDialogIdRepository>()
        val periodeId = UUID.randomUUID()
        val egenvurderingId = UUID.randomUUID()

        every { periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId) } returns null
        every {
            periodeIdDialogIdRepository.insert(
                periodeId = periodeId,
                dialogId = null,
                egenvurderingId = egenvurderingId,
                httpStatusCode = HttpStatusCode.Conflict,
                errorMessage = "Bruker kan ikke varsles",
            )
        } returns Unit

        val engine = MockEngine {
            respond(
                content = "Bruker kan ikke varsles",
                status = HttpStatusCode.Conflict,
            )
        }
        val veilarbdialogClient = VeilarbdialogClient(
            config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake", target = "veilarbdialog.fake"),
            texasClient = mockk(relaxed = true),
            httpClient = testClient(engine)
        )
        val service = DialogService(
            applicationConfig = applicationConfig,
            veilarbdialogClient = veilarbdialogClient,
            periodeIdDialogIdRepository = periodeIdDialogIdRepository
        )

        val egenvurdering = egenvurdering(
            periodeId = periodeId,
            egenvurderingId = egenvurderingId,
            navProfilering = ANTATT_BEHOV_FOR_VEILEDNING,
            brukersEgenvurdering = ANTATT_BEHOV_FOR_VEILEDNING,
            tidspunkt = Instant.parse("2025-03-16T12:00:00Z"),
            fnr = "10987654321"
        )

        val records = consumerRecordsOf(egenvurdering)
        service.varsleVeilederOmEgenvurderingAvProfilering(records)

        verify(exactly = 1) { periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId) }
        verify(exactly = 1) {
            periodeIdDialogIdRepository.insert(
                periodeId = periodeId,
                dialogId = null,
                egenvurderingId = egenvurderingId,
                httpStatusCode = HttpStatusCode.Conflict,
                errorMessage = "Bruker kan ikke varsles",
            )
        }

        confirmVerified(periodeIdDialogIdRepository)
    }

    "DialogId i db er ulik dialogId fra veilarbdialog – Id i db oppdateres" {
        val dialogIdDb = 888L
        val dialogIdFraVeilarb = 999L
        val periodeId = UUID.randomUUID()

        val periodeIdDialogIdRepository = mockk<PeriodeIdDialogIdRepository>()
        every { periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId) } returns PeriodeDialogRow(
            periodeId = periodeId,
            dialogId = dialogIdDb,
            egenvurderingId = UUID.randomUUID(),
            dialogHttpStatusCode = HttpStatusCode.OK.value,
            dialogErrorMessage = null,
        )
        every { periodeIdDialogIdRepository.update(periodeId, dialogIdFraVeilarb) } just runs

        val engine = MockEngine {
            respond(
                content = """{"id":"$dialogIdFraVeilarb"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, Application.Json.toString())
            )
        }
        val veilarbdialogClient = VeilarbdialogClient(
            config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake", target = "veilarbdialog.fake"),
            texasClient = mockk(relaxed = true),
            httpClient = testClient(engine)
        )
        val service = DialogService(
            applicationConfig = applicationConfig,
            veilarbdialogClient = veilarbdialogClient,
            periodeIdDialogIdRepository = periodeIdDialogIdRepository
        )

        val egenvurdering = egenvurdering(
            periodeId = periodeId,
            navProfilering = ANTATT_GODE_MULIGHETER,
            brukersEgenvurdering = ANTATT_GODE_MULIGHETER,
            tidspunkt = Instant.parse("2025-03-20T09:00:00Z"),
            fnr = "55555555555"
        )

        val records = consumerRecordsOf(egenvurdering)
        service.varsleVeilederOmEgenvurderingAvProfilering(records)

        verify { periodeIdDialogIdRepository.hentPeriodeIdDialogIdInfo(periodeId) }
        verify(exactly = 0) { periodeIdDialogIdRepository.insert(any(), any(), any(), any(), any()) }
        verify { periodeIdDialogIdRepository.update(periodeId, dialogIdFraVeilarb) }
        confirmVerified(periodeIdDialogIdRepository)
    }
})

fun consumerRecordsOf(egenvurdering: Egenvurdering): ConsumerRecords<Long, Egenvurdering> {
    val topic = "egenvurdering"
    val record = ConsumerRecord(topic, 0, 0L, 1L, egenvurdering)
    val topicPartition = TopicPartition(topic, 0)
    return ConsumerRecords(mapOf(topicPartition to listOf(record)), emptyMap())
}

fun egenvurdering(
    periodeId: UUID = UUID.randomUUID(),
    egenvurderingId: UUID = UUID.randomUUID(),
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
        egenvurderingId,
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
