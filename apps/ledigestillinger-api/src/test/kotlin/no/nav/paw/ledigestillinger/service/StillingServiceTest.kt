package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.paw.ledigestillinger.api.models.Fylke
import no.nav.paw.ledigestillinger.api.models.Kommune
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.model.shouldBeEqualTo
import no.nav.paw.ledigestillinger.test.TestContext
import no.nav.paw.ledigestillinger.test.TestData
import no.nav.paw.ledigestillinger.test.selectRows

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
                TestData.message2_3,
                TestData.message2_4
            )

            // WHEN
            stillingService.handleMessages(messages.asSequence())

            // THEN
            val rows = StillingerTable.selectRows()
            rows shouldHaveSize 6
            val row1 = rows[0]
            val row2 = rows[1]
            val row3 = rows[2]
            val row4 = rows[3]
            val row5 = rows[4]
            val row6 = rows[5]
            row1.uuid shouldBe TestData.message1_1.key
            row1 shouldBeEqualTo TestData.message1_1.value
            row2.uuid shouldBe TestData.message1_2.key
            row2 shouldBeEqualTo TestData.message1_2.value
            row3.uuid shouldBe TestData.message2_1.key
            row3 shouldBeEqualTo TestData.message2_1.value
            row4.uuid shouldBe TestData.message2_2.key
            row4 shouldBeEqualTo TestData.message2_2.value
            row5.uuid shouldBe TestData.message2_3.key
            row5 shouldBeEqualTo TestData.message2_3.value
            row6.uuid shouldBe TestData.message2_4.key
            row6 shouldBeEqualTo TestData.message2_4.value

            // WHEN
            val stillinger = stillingService.finnStillinger(
                soekeord = emptyList(),
                fylker = listOf(
                    Fylke(
                        fylkesnummer = "57",
                        kommuner = listOf(
                            Kommune(
                                kommunenummer = "5701"
                            ),
                            Kommune(
                                kommunenummer = "5704"
                            )
                        )
                    )
                ),
                kategorier = listOf("2011", "2012", "2014")
            )

            // THEN
            stillinger shouldHaveSize 2
            stillinger shouldContainOnly listOf(
                TestData.message2_1.value.asDto(),
                TestData.message2_4.value.asDto()
            )
        }
    }
})
