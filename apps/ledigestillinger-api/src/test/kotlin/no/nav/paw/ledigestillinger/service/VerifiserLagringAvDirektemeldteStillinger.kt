package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.opentelemetry.api.trace.Span
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.pam.stilling.ext.avro.Property
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.asStillingRow
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.test.TestContext
import no.nav.paw.ledigestillinger.test.TestData
import no.nav.paw.ledigestillinger.test.TestData.baseAd
import no.nav.paw.ledigestillinger.test.selectRows
import no.naw.paw.ledigestillinger.model.Fylke
import no.naw.paw.ledigestillinger.model.Paging
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.UUID

class VerifiserLagringAvDirektemeldteStillinger : FreeSpec({
    with(TestContext.buildWithDatabase()) {

        beforeSpec {
            setUp()
        }

        afterSpec {
            tearDown()
        }

        "Direktemeldte stillinger skal lagres i databasen og kunne søkes opp" - {
            val ad1 = baseAd.apply {
                source = "DIR"
                properties = listOf(
                    Property("direktemeldtStillingskategori", "STILLING"),
                    Property("title", "Direktemeldt stilling 1")
                )
            }
            val ad2 = baseAd.apply {
                source = "DiR"
                properties = listOf(
                    Property("DirektemeldtStillingsKategori", "sTILLINg"),
                    Property("title", "Direktemeldt stilling 2")
                )
            }
            val raderSomSkalSkrives = listOf(ad1, ad2)
                .map {
                    Message(
                        span = Span.current(),
                        key = UUID.fromString(it.uuid),
                        value = it,
                        topic = "test-topic",
                        partition = 0,
                        offset = 0,
                        timestamp = Instant.now()
                    )
                }
            "Vi skriver direktemeldte stillinger til databasen" {
                transaction {
                    raderSomSkalSkrives.forEach {
                        println("Skriver Ad: ${it.value.uuid} med source ${it.value.source} og properties ${it.value.properties}")
                        val rad = it.asStillingRow()
                        println("Som rad: ${rad.uuid} med tags $rad")
                        stillingService.lagreStilling(rad)
                    }
                }
            }
            raderSomSkalSkrives.forEach { stilling ->
                "Vi kan lese tilbake direktemeldt stilling med uuid ${stilling.value.uuid}" {
                    val rad = transaction {
                        StillingerTable.selectRowByUUID(stilling.key)
                    }
                    rad.shouldNotBeNull()
                    rad.uuid shouldBe stilling.key
                    rad.tags shouldContainOnly listOf(Tag.DIREKTEMELDT_V1)
                }
            }

        }
    }
})
