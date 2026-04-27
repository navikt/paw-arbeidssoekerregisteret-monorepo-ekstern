package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.test.TestContext
import no.nav.paw.ledigestillinger.test.TestData
import no.naw.paw.ledigestillinger.model.Paging
import no.naw.paw.ledigestillinger.model.Tag
import java.util.*

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
            val message1: Message<UUID, Ad> = TestData.message(
                source = "DIR",
                properties = listOf(
                    no.nav.pam.stilling.ext.avro.Property("direktemeldtStillingskategori", "STILLING")
                )
            )
            val message2: Message<UUID, Ad> = TestData.message(
                source = "DIR",
                properties = listOf(
                    no.nav.pam.stilling.ext.avro.Property("DIREKTEMELDTSTILLINGSKATEGORI", "stilling")
                )
            )
            val message3: Message<UUID, Ad> = TestData.message(
                source = "DIR",
                properties = listOf(
                    no.nav.pam.stilling.ext.avro.Property("direktemeldtStillingskategori", "ANNET")
                )
            )
            val message4: Message<UUID, Ad> = TestData.message(
                source = "ANNET",
                properties = listOf(
                    no.nav.pam.stilling.ext.avro.Property("direktemeldtStillingskategori", "ANNET")
                )
            )
            val messages = listOf(message1, message2, message3, message4)

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
                    stillinger.first().uuid shouldBe message4.key
                    stillinger.first().tags shouldBe emptyList()
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
                        stillingerForFilter.find { it.uuid == message1.key } should { stilling ->
                            stilling.shouldNotBeNull()
                            stilling.uuid shouldBe message1.key
                            stilling shouldBe message1.value.asDto()
                        }
                    }
                    "Direkte meldt stilling 2 skal være i resultatet" {
                        stillingerForFilter.find { it.uuid == message2.key } should { stilling ->
                            stilling.shouldNotBeNull()
                            stilling.uuid shouldBe message2.key
                            stilling shouldBe message2.value.asDto()
                        }
                    }
                }
            }
        }
    }
})