package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.pam.stilling.ext.avro.AdStatus
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.test.TestContext
import no.nav.paw.ledigestillinger.test.TestData
import no.nav.paw.ledigestillinger.test.selectRows
import no.naw.paw.ledigestillinger.model.Fylke
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

class StillingServiceTest : FreeSpec({
    with(TestContext.buildWithDatabase()) {

        beforeSpec {
            setUp()
        }

        afterSpec {
            tearDown()
        }

        "Skal motta stillinger fra Kafka og lagre dem i databasen og deretter søke de opp" {
            // GIVEN
            val messages = listOf(
                TestData.message1_1,
                TestData.message1_2,
                TestData.message2_1,
                TestData.message2_2,
                TestData.message3_1,
                TestData.message3_2,
                TestData.message4_1,
                TestData.message4_2,
                TestData.message5_1,
                TestData.message5_2,
                TestData.message5_3,
                TestData.message5_4,
                TestData.message5_5,
            )

            // WHEN
            stillingService.handleMessages(messages.asSequence())

            // THEN
            val rows = StillingerTable.selectRows()
            rows shouldHaveSize 13
            val row1 = rows[0]
            val row2 = rows[1]
            val row3 = rows[2]
            val row4 = rows[3]
            val row5 = rows[4]
            val row6 = rows[5]
            val row7 = rows[6]
            val row8 = rows[7]
            val row9 = rows[8]
            val row10 = rows[9]
            val row11 = rows[10]
            val row12 = rows[11]
            val row13 = rows[12]
            row1.uuid shouldBe TestData.message1_1.key
            row1.asDto() shouldBe TestData.message1_1.value.asDto()
            row2.uuid shouldBe TestData.message1_2.key
            row2.asDto() shouldBe TestData.message1_2.value.asDto()
            row3.uuid shouldBe TestData.message2_1.key
            row3.asDto() shouldBe TestData.message2_1.value.asDto()
            row4.uuid shouldBe TestData.message2_2.key
            row4.asDto() shouldBe TestData.message2_2.value.asDto()
            row5.uuid shouldBe TestData.message3_1.key
            row5.asDto() shouldBe TestData.message3_1.value.asDto()
            row6.uuid shouldBe TestData.message3_2.key
            row6.asDto() shouldBe TestData.message3_2.value.asDto()
            row7.uuid shouldBe TestData.message4_1.key
            row7.asDto() shouldBe TestData.message4_1.value.asDto()
            row8.uuid shouldBe TestData.message4_2.key
            row8.asDto() shouldBe TestData.message4_2.value.asDto()
            row9.uuid shouldBe TestData.message5_1.key
            row9.asDto() shouldBe TestData.message5_1.value.asDto()
            row10.uuid shouldBe TestData.message5_2.key
            row10.asDto() shouldBe TestData.message5_2.value.asDto()
            row11.uuid shouldBe TestData.message5_3.key
            row11.asDto() shouldBe TestData.message5_3.value.asDto()
            row12.uuid shouldBe TestData.message5_4.key
            row12.asDto() shouldBe TestData.message5_4.value.asDto()
            row13.uuid shouldBe TestData.message5_5.key
            row13.asDto() shouldBe TestData.message5_5.value.asDto()

            // WHEN
            val stillinger = stillingService.finnStillingerByEgenskaper(
                soekeord = emptyList(),
                fylker = listOf(
                    Fylke(
                        fylkesnummer = "10",
                        kommuner = emptyList()
                    ),
                    Fylke(
                        fylkesnummer = "30",
                        kommuner = emptyList()
                    )
                ),
                kategorier = listOf("1012", "3011")
            )

            // THEN
            stillinger shouldHaveSize 2
            stillinger shouldContainOnly listOf(
                TestData.message1_2.value.asDto(),
                TestData.message3_1.value.asDto()
            )
        }

        "Skal slette stillinger som ikke er aktive og aldre enn X".config(enabled = false) {
            // GIVEN
            val medUtloeperEldreEnn = Duration.ofDays(30)
            val messages: List<Message<UUID, Ad>> = AdStatus.entries.flatMap { status ->
                listOf(
                    TestData.message(
                        status = status,
                        published = LocalDateTime.now().minusDays(medUtloeperEldreEnn.toDays() + 100),
                        expires = null
                    ),
                    TestData.message(
                        status = status,
                        published = LocalDateTime.now().minusDays(medUtloeperEldreEnn.toDays() + 100),
                        expires = LocalDateTime.now().minusDays(medUtloeperEldreEnn.toDays() + 20)
                    ),
                    TestData.message(
                        status = status,
                        published = LocalDateTime.now().minusDays(medUtloeperEldreEnn.toDays() + 100),
                        expires = LocalDateTime.now().minusDays(medUtloeperEldreEnn.toDays() + 40)
                    )
                )
            }

            // WHEN
            stillingService.handleMessages(messages.asSequence())
            val rows1 = StillingerTable.selectRows()

            // THEN
            rows1 shouldHaveSize 15
            rows1.map { it.asDto() } shouldContainOnly messages.map { it.value.asDto() }

            // WHEN
            val rowsAffected = stillingService.slettStillinger(medUtloeperEldreEnn)
            val rows2 = StillingerTable.selectRows()

            // THEN
            rowsAffected shouldBe 7
            rows2 shouldHaveSize 15
            rows2.map { it.asDto() } shouldContainOnly messages.map { it.value.asDto() }
        }
    }
})
