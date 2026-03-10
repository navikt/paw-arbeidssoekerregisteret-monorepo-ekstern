package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.opentelemetry.api.trace.Span
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.pam.stilling.ext.avro.Property
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.model.asStillingRow
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.test.TestContext
import no.nav.paw.ledigestillinger.test.TestData.baseAd
import no.naw.paw.ledigestillinger.model.Tag
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.util.*

class VerifiserLagringAvDirektemeldteStillinger : FreeSpec({
    with(TestContext.buildWithDatabase()) {

        beforeSpec {
            setUp()
        }

        afterSpec {
            tearDown()
        }

        "Direktemeldte stillinger skal lagres i databasen og kunne søkes opp" - {
            val ad1 = baseAd().apply {
                uuid = UUID.randomUUID().toString()
                source = "DIR"
                properties = listOf(
                    Property("direktemeldtStillingskategori", "STILLING"),
                    Property("title", "Direktemeldt stilling 1")
                )
            }
            val ad2 = baseAd().apply {
                uuid = UUID.randomUUID().toString()
                source = "DiR"
                properties = listOf(
                    Property("DirektemeldtStillingsKategori", "sTILLINg"),
                    Property("title", "Direktemeldt stilling 2")
                )
            }
            val ad3 = baseAd().apply {
                uuid = UUID.randomUUID().toString()
                source = "STILLINGSDATASYSTEM"
                properties = listOf(
                    Property("direktemeldtStillingskategori", "NEI"),
                    Property("title", "Ikke direktemeldt stilling")
                )
            }
            val ad3_updated = baseAd().apply {
                uuid = ad3.uuid
                source = "DIR"
                properties = listOf(
                    Property("direktemeldtStillingskategori", "STILLING"),
                    Property("title", "Oppdatert til direktemeldt stilling")
                )
            }
            val raderSomSkalSkrives = listOf(ad1, ad2, ad3)
                .map(::message)
            "Vi skriver direktemeldte stillinger til databasen" {
                raderSomSkalSkrives.forEach {
                    transaction {
                        println("Skriver Ad: ${it.value.uuid} med source ${it.value.source} og properties ${it.value.properties}")
                        val rad = it.asStillingRow()
                        println("Som rad: ${rad.uuid} med tags $rad")
                        stillingService.lagreStilling(rad)
                    }
                }
            }
            listOf(ad1, ad2).forEach { stilling ->
                "Vi kan lese tilbake direktemeldt stilling med uuid ${stilling.uuid}" {
                    val rad = transaction {
                        StillingerTable.selectRowByUUID(UUID.fromString(stilling.uuid))
                    }
                    rad.shouldNotBeNull()
                    rad.uuid shouldBe UUID.fromString(stilling.uuid)
                    rad.tags shouldContainOnly listOf(Tag.DIREKTEMELDT_V1)
                }
            }
            "Ad3 skal ikke være lagret som direktemeldt før oppdatering" {
                val rad = transaction {
                    StillingerTable.selectRowByUUID(UUID.fromString(ad3.uuid))
                }
                rad.shouldNotBeNull()
                rad.uuid shouldBe UUID.fromString(ad3.uuid)
                rad.tags shouldContainOnly emptyList()
            }
            "Etter oppdatering skal ad3 være lagret som direktemeldt" {
                transaction {
                    val rad = message(ad3_updated)
                    stillingService.lagreStilling(rad.asStillingRow())
                }
                val rad = transaction {
                    StillingerTable.selectRowByUUID(UUID.fromString(ad3_updated.uuid))
                }
                rad.shouldNotBeNull()
                rad.uuid shouldBe UUID.fromString(ad3_updated.uuid)
                rad.tags shouldContainOnly listOf(Tag.DIREKTEMELDT_V1)
        }
    }
}})

private fun message(ad: Ad): Message<UUID, Ad> = Message(
    span = Span.current(),
    key = UUID.fromString(ad.uuid)!!,
    value = ad,
    topic = "test-topic",
    partition = 0,
    offset = 0,
    timestamp = Instant.now()
)
