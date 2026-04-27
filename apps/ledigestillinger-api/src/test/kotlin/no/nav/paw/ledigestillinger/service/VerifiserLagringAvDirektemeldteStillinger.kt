package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.pam.stilling.ext.avro.Property
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.test.TestContext
import no.nav.paw.ledigestillinger.test.TestData
import no.naw.paw.ledigestillinger.model.Tag
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
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
            val message1: Message<UUID, Ad> = TestData.message(
                source = "DIR",
                properties = listOf(
                    Property("direktemeldtStillingskategori", "STILLING"),
                    Property("title", "Direktemeldt stilling 1")
                )
            )
            val message2: Message<UUID, Ad> = TestData.message(
                source = "DiR",
                properties = listOf(
                    Property("DirektemeldtStillingsKategori", "sTILLINg"),
                    Property("title", "Direktemeldt stilling 2")
                )
            )
            val message3_1: Message<UUID, Ad> = TestData.message(
                source = "STILLINGSDATASYSTEM",
                properties = listOf(
                    Property("direktemeldtStillingskategori", "NEI"),
                    Property("title", "Ikke direktemeldt stilling")
                )
            )
            val message3_2: Message<UUID, Ad> = TestData.message(
                uuid = message3_1.key,
                source = "DIR",
                properties = listOf(
                    Property("direktemeldtStillingskategori", "STILLING"),
                    Property("title", "Oppdatert til direktemeldt stilling")
                )
            )
            val messages = listOf(message1, message2, message3_1)

            "Vi skriver direktemeldte stillinger til databasen" {
                stillingService.handleMessages(messages.asSequence())
            }

            listOf(message1, message2)
                .map { it.key }
                .forEach { uuid ->
                    "Vi kan lese tilbake direktemeldt stilling med uuid $uuid" {
                        val rad = transaction {
                            StillingerTable.selectRowByUUID(uuid)
                        }
                        rad.shouldNotBeNull()
                        rad.uuid shouldBe uuid
                        rad.tags shouldContainOnly listOf(Tag.DIREKTEMELDT_V1)
                    }
                }

            "Ad3 skal ikke være lagret som direktemeldt før oppdatering" {
                val rad = transaction {
                    StillingerTable.selectRowByUUID(message3_1.key)
                }
                rad.shouldNotBeNull()
                rad.uuid shouldBe message3_1.key
                rad.tags shouldBe emptyList()
            }

            "Etter oppdatering skal ad3 være lagret som direktemeldt" {
                stillingService.handleMessages(listOf(message3_2).asSequence())

                val rad = transaction {
                    StillingerTable.selectRowByUUID(message3_1.key)
                }
                rad.shouldNotBeNull()
                rad.uuid shouldBe message3_1.key
                rad.tags shouldContainOnly listOf(Tag.DIREKTEMELDT_V1)
            }
        }
    }
})