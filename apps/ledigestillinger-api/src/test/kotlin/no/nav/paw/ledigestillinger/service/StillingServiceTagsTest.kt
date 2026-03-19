package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.opentelemetry.api.trace.Span
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.test.TestContext
import no.nav.paw.ledigestillinger.test.TestData
import no.nav.paw.ledigestillinger.test.TestData.baseAd
import no.nav.paw.ledigestillinger.test.selectRows
import no.naw.paw.ledigestillinger.model.Fylke
import no.naw.paw.ledigestillinger.model.Paging
import no.naw.paw.ledigestillinger.model.Tag
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

class StillingServiceTagsTest : FreeSpec({
    with(TestContext.buildWithDatabase()) {

        beforeSpec {
            setUp()
        }

        afterSpec {
            tearDown()
        }

        "Skal motta stillinger fra Kafka og lagre dem i databasen og deretter søke de opp" - {
            // GIVEN
            val direkteMeldt_1 = baseAd().apply {
                uuid = UUID.randomUUID().toString()
                source = "DIR"
                properties = listOf(
                    no.nav.pam.stilling.ext.avro.Property("direktemeldtStillingskategori", "STILLING")
                )
            }
            println("direkteMeldt_1: ${direkteMeldt_1.uuid}")
            val direkteMeldt_2 = baseAd().apply {
                uuid = UUID.randomUUID().toString()
                source = "DIR"
                properties = listOf(
                    no.nav.pam.stilling.ext.avro.Property("DIREKTEMELDTSTILLINGSKATEGORI", "stilling")
                )
            }
            println("direkteMeldt_2: ${direkteMeldt_2.uuid}")
            val ikkeDirekteMeldt = baseAd().apply {
                uuid = UUID.randomUUID().toString()
                source = "DIR"
                properties = listOf(
                    no.nav.pam.stilling.ext.avro.Property("direktemeldtStillingskategori", "ANNET")
                )
            }
            println("ikkeDirekteMeldt: ${ikkeDirekteMeldt.uuid}")
            val ikkeDirekteMeldtEllerDir = baseAd().apply {
                uuid = UUID.randomUUID().toString()
                source = "ANNET"
                properties = listOf(
                    no.nav.pam.stilling.ext.avro.Property("direktemeldtStillingskategori", "ANNET")
                )
            }
            println("ikkeDirekteMeldtEllerDir: ${ikkeDirekteMeldtEllerDir.uuid}")
            val messages = listOf(direkteMeldt_2, ikkeDirekteMeldt, ikkeDirekteMeldtEllerDir, direkteMeldt_1)
                .map(::toMessage)
            "Alle stilling skrives til db uten feil" {
                stillingService.handleMessages(messages.asSequence())
            }

            "Vi henter ut en liste med stillinger uten tags, da skal ikke de med DIR være med" - {
                val stillinger = stillingService.finnStillingerByEgenskaper(
                    medSoekeord = emptyList(),
                    medFylker = emptyList(),
                    medStyrkkoder = emptyList(),
                    paging = Paging(1, 100),
                    tags = emptyList()
                )

                "Vi skal ha 1 stilling i databasen uten kilde DIR" {
                    stillinger shouldHaveSize 1
                    stillinger.first().uuid.toString() shouldBe ikkeDirekteMeldtEllerDir.uuid
                    stillinger.first().tags.shouldBeEmpty()
                }
            }
            "Vi søker etter stillinger med taggen DIREKTEMELDT_V1 og sjekker at vi får tilbake de riktige stillingene" - {
                val stillingerForFilter = stillingService.finnStillingerByEgenskaper(
                    medSoekeord = emptyList(),
                    medFylker = emptyList(),
                    medStyrkkoder = emptyList(),
                    paging = Paging(1, 100),
                    tags = setOf(Tag.DIREKTEMELDT_V1)
                )
                "Vi skal få tilbake 2 stillinger" {
                    stillingerForFilter shouldHaveSize 2
                }
                stillingerForFilter.forEach { stilling ->
                    println("Stilling med UUID ${stilling.uuid} har tags ${stilling.tags}")
                }
                "Vi skal få tilbake de to stillingene som er direktemeldt" - {
                    "Direkte meldt stilling 1 skal være i resultatet" {
                        stillingerForFilter.find { it.uuid.toString() == direkteMeldt_1.uuid } should { stilling ->
                            stilling.shouldNotBeNull()
                            stilling.uuid shouldBe UUID.fromString(direkteMeldt_1.uuid)
                            stilling shouldBe direkteMeldt_1.asDto().copy(tags = listOf(Tag.DIREKTEMELDT_V1))
                        }
                    }
                    "Direkte meldt stilling 2 skal være i resultatet" {
                        stillingerForFilter.find { it.uuid.toString() == direkteMeldt_2.uuid } should { stilling ->
                            stilling.shouldNotBeNull()
                            stilling.uuid shouldBe UUID.fromString(direkteMeldt_2.uuid)
                            stilling shouldBe direkteMeldt_2.asDto().copy(tags = listOf(Tag.DIREKTEMELDT_V1))
                        }
                    }
                }
            }
        }
    }
})

private val offset = AtomicLong(0)
fun toMessage(
    ad: Ad,
    timestamp: Instant = Instant.now(),
): Message<UUID, Ad> {
    return Message(
        span = Span.current(),
        key = UUID.fromString(ad.uuid),
        value = ad,
        topic = "test-topic",
        partition = 0,
        offset = offset.getAndIncrement(),
        timestamp = timestamp
    )
}
