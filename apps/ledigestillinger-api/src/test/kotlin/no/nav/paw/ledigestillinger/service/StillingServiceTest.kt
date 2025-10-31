package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.test.TestContext
import no.nav.paw.ledigestillinger.test.TestData
import no.nav.paw.ledigestillinger.test.selectRows
import no.naw.paw.ledigestillinger.model.Fylke

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
                TestData.message4_2
            )

            // WHEN
            stillingService.handleMessages(messages.asSequence())

            // THEN
            val rows = StillingerTable.selectRows()
            rows shouldHaveSize 8
            val row1 = rows[0]
            val row2 = rows[1]
            val row3 = rows[2]
            val row4 = rows[3]
            val row5 = rows[4]
            val row6 = rows[5]
            val row7 = rows[6]
            val row8 = rows[7]
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
    }
})
