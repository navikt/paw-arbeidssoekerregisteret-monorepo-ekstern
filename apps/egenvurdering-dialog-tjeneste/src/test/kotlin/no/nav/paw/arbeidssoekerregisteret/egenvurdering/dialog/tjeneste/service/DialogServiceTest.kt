package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.mockk.mockk
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.client.VeilarbdialogClient
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.config.VeilarbdialogClientConfig
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeIdDialogIdTable
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.repository.PeriodeIdDialogIdTable.getByWithAudit
import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.test.buildPostgresDataSource
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
import org.jetbrains.exposed.v1.jdbc.Database
import java.time.Instant
import java.util.*

class DialogServiceTest : FreeSpec({

    val dataSource = autoClose(buildPostgresDataSource())
    beforeSpec { Database.connect(dataSource) }

    "Egenvurderinger innenfor samme periode" - {
        val periodeId = UUID.randomUUID()
        val førsteEgenvurderingId = UUID.randomUUID()
        val dialogId = 555L

        "Ny tråd opprettes for første egenvurdering i en periode" {
            val engine = MockEngine {
                respond(
                    content = """{"id":"$dialogId"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(ContentType, Json.toString())
                )
            }
            val veilarbdialogClient = VeilarbdialogClient(
                config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake", target = "veilarbdialog.fake"),
                texasClient = mockk(relaxed = true),
                httpClient = testClient(engine)
            )
            val service = DialogService(
                veilarbdialogClient = veilarbdialogClient
            )

            val egenvurdering = egenvurdering(
                periodeId = periodeId,
                navProfilering = ANTATT_GODE_MULIGHETER,
                egenvurderingId = førsteEgenvurderingId,
                brukersEgenvurdering = ANTATT_BEHOV_FOR_VEILEDNING,
                tidspunkt = Instant.parse("2025-03-15T10:00:00Z"),
                fnr = "12345678901"
            )

            val records = consumerRecordsOf(egenvurdering)
            service.varsleVeilederOmEgenvurderingAvProfilering(records)

            getByWithAudit(periodeId).let { periodeDialogRow ->
                periodeDialogRow.shouldNotBeNull()
                periodeDialogRow.periodeId shouldBe periodeId
                periodeDialogRow.dialogId shouldBe dialogId
                periodeDialogRow.periodeDialogAuditRows shouldHaveSize 1
                periodeDialogRow.finnSisteAuditRow()?.let { auditRow ->
                    auditRow.egenvurderingId shouldBe førsteEgenvurderingId
                    auditRow.dialogHttpStatusCode shouldBe HttpStatusCode.OK.value
                    auditRow.dialogErrorMessage shouldBe null
                }
            }
        }

        val andreEgenvurderingId = UUID.randomUUID()

        "Oppdater eksisterende tråd ved ny egenvurdering i samme periode" {
            val engine = MockEngine {
                respond(
                    content = """{"id":"$dialogId"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(ContentType, Json.toString())
                )
            }
            val veilarbdialogClient = VeilarbdialogClient(
                config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake", target = "veilarbdialog.fake"),
                texasClient = mockk(relaxed = true),
                httpClient = testClient(engine)
            )
            val service = DialogService(
                veilarbdialogClient = veilarbdialogClient
            )

            val nyEgenvurdering = egenvurdering(
                periodeId = periodeId,
                navProfilering = ANTATT_GODE_MULIGHETER,
                egenvurderingId = andreEgenvurderingId,
                brukersEgenvurdering = ANTATT_GODE_MULIGHETER,
                tidspunkt = Instant.parse("2025-03-20T09:00:00Z"),
                fnr = "55555555555"
            )

            service.varsleVeilederOmEgenvurderingAvProfilering(consumerRecordsOf(nyEgenvurdering))

            getByWithAudit(periodeId).let { periodeDialogRow ->
                periodeDialogRow.shouldNotBeNull()
                periodeDialogRow.periodeId shouldBe periodeId
                periodeDialogRow.dialogId shouldBe dialogId
                periodeDialogRow.periodeDialogAuditRows shouldHaveSize 2
                periodeDialogRow.finnSisteAuditRow()?.let { auditRow ->
                    auditRow.egenvurderingId shouldBe andreEgenvurderingId
                    auditRow.dialogHttpStatusCode shouldBe HttpStatusCode.OK.value
                    auditRow.dialogErrorMessage shouldBe null
                }
            }
        }

        val tredjeEgenvurderingId = UUID.randomUUID()
        "Oppfølgingsperiode avsluttet setter riktig statuskode og feilmelding fra veilarbdialog" {
            val errorMessage = "Kan ikke sende henvendelse på historisk dialog"
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
                veilarbdialogClient = veilarbdialogClient
            )

            val nyEgenvurdering = egenvurdering(
                periodeId = periodeId,
                navProfilering = ANTATT_GODE_MULIGHETER,
                egenvurderingId = tredjeEgenvurderingId,
                brukersEgenvurdering = ANTATT_GODE_MULIGHETER,
                tidspunkt = Instant.parse("2025-03-20T09:00:00Z"),
                fnr = "55555555555"
            )

            service.varsleVeilederOmEgenvurderingAvProfilering(consumerRecordsOf(nyEgenvurdering))

            getByWithAudit(periodeId).let { periodeDialogRow ->
                periodeDialogRow.shouldNotBeNull()
                periodeDialogRow.periodeId shouldBe periodeId
                periodeDialogRow.dialogId shouldBe dialogId
                periodeDialogRow.periodeDialogAuditRows shouldHaveSize 3
                periodeDialogRow.finnSisteAuditRow()?.let { auditRow ->
                    auditRow.egenvurderingId shouldBe tredjeEgenvurderingId
                    auditRow.dialogHttpStatusCode shouldBe HttpStatusCode.Conflict.value
                    auditRow.dialogErrorMessage shouldBe errorMessage
                }
            }
        }
    }

    "Feil fra veilarbdialog løser seg" - {
        val periodeId = UUID.randomUUID()
        val egenvurderingId = UUID.randomUUID()
        val egenvurdering = egenvurdering(
            periodeId = periodeId,
            egenvurderingId = egenvurderingId,
            navProfilering = ANTATT_BEHOV_FOR_VEILEDNING,
            brukersEgenvurdering = ANTATT_BEHOV_FOR_VEILEDNING,
            tidspunkt = Instant.parse("2025-03-16T12:00:00Z"),
            fnr = "10987654321"
        )

        "Feil lagres i db og exception kastes slik at appen går ned" {
            val errorMessage = "Feil ved kall mot eksternt system"
            val engine = MockEngine {
                respond(
                    content = errorMessage,
                    status = HttpStatusCode.InternalServerError,
                )
            }
            val veilarbdialogClient = VeilarbdialogClient(
                config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake", target = "veilarbdialog.fake"),
                texasClient = mockk(relaxed = true),
                httpClient = testClient(engine)
            )
            val service = DialogService(
                veilarbdialogClient = veilarbdialogClient
            )

            val records = consumerRecordsOf(egenvurdering)
            shouldThrow<Exception> {
                service.varsleVeilederOmEgenvurderingAvProfilering(records)
            }

            getByWithAudit(periodeId).let { periodeDialogRow ->
                periodeDialogRow.shouldNotBeNull()
                periodeDialogRow.periodeId shouldBe periodeId
                periodeDialogRow.dialogId shouldBe null
                periodeDialogRow.periodeDialogAuditRows shouldHaveSize 1
                periodeDialogRow.finnSisteAuditRow()?.let { auditRow ->
                    auditRow.egenvurderingId shouldBe egenvurderingId
                    auditRow.dialogHttpStatusCode shouldBe HttpStatusCode.InternalServerError.value
                    auditRow.dialogErrorMessage shouldContain errorMessage
                }
            }
        }

        "Feilen rettes, og retry av egenvurdering melding fører til ny dialog" {
            val dialogId = 3456L
            val engine = MockEngine {
                respond(
                    content = """{"id":"$dialogId"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(ContentType, Json.toString())
                )
            }
            val veilarbdialogClient = VeilarbdialogClient(
                config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake", target = "veilarbdialog.fake"),
                texasClient = mockk(relaxed = true),
                httpClient = testClient(engine)
            )
            val service = DialogService(
                veilarbdialogClient = veilarbdialogClient
            )

            val records = consumerRecordsOf(egenvurdering)
            service.varsleVeilederOmEgenvurderingAvProfilering(records)

            getByWithAudit(periodeId).let { periodeDialogRow ->
                periodeDialogRow.shouldNotBeNull()
                periodeDialogRow.periodeId shouldBe periodeId
                periodeDialogRow.dialogId shouldBe dialogId
                periodeDialogRow.periodeDialogAuditRows shouldHaveSize 2
                periodeDialogRow.finnSisteAuditRow()?.let { auditRow ->
                    auditRow.egenvurderingId shouldBe egenvurderingId
                    auditRow.dialogHttpStatusCode shouldBe HttpStatusCode.OK.value
                    auditRow.dialogErrorMessage shouldBe null
                }
            }
        }
    }

    "Person har reservert seg i kontakt og reservasjonsregisteret (KRR)" {
        val errorMessage = "Bruker kan ikke varsles"
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
            veilarbdialogClient = veilarbdialogClient
        )

        val periodeId = UUID.randomUUID()
        val egenvurderingId = UUID.randomUUID()
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

        getByWithAudit(periodeId).let { periodeDialogRow ->
            periodeDialogRow.shouldNotBeNull()
            periodeDialogRow.periodeId shouldBe periodeId
            periodeDialogRow.dialogId shouldBe null
            periodeDialogRow.periodeDialogAuditRows shouldHaveSize 1
            periodeDialogRow.finnSisteAuditRow()?.let { auditRow ->
                auditRow.egenvurderingId shouldBe egenvurderingId
                auditRow.dialogHttpStatusCode shouldBe HttpStatusCode.Conflict.value
                auditRow.dialogErrorMessage shouldBe errorMessage
            }
        }
    }

    "DialogId i db er ulik dialogId fra veilarbdialog – Id i db oppdateres" {
        val dialogIdDb = 12345L
        val dialogIdFraVeilarb = 999L
        val periodeId = UUID.randomUUID()

        PeriodeIdDialogIdTable.insert(
            periodeId = periodeId,
            dialogId = dialogIdDb,
            egenvurderingId = UUID.randomUUID(),
            httpStatusCode = HttpStatusCode.OK,
        )

        val engine = MockEngine {
            respond(
                content = """{"id":"$dialogIdFraVeilarb"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(ContentType, Json.toString())
            )
        }
        val veilarbdialogClient = VeilarbdialogClient(
            config = VeilarbdialogClientConfig(url = "http://veilarbdialog.fake", target = "veilarbdialog.fake"),
            texasClient = mockk(relaxed = true),
            httpClient = testClient(engine)
        )
        val service = DialogService(
            veilarbdialogClient = veilarbdialogClient
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

        getByWithAudit(periodeId).let { row ->
            row.shouldNotBeNull()
            row.dialogId shouldBe dialogIdFraVeilarb
        }
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
