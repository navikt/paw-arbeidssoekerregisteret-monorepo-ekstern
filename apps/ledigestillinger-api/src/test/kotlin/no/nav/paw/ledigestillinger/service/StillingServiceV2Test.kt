package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.dao.StillingerTableV2
import no.nav.paw.ledigestillinger.test.TestContext
import no.nav.paw.ledigestillinger.test.TestData
import no.nav.paw.ledigestillinger.test.selectRows
import no.naw.paw.ledigestillinger.model.Fylke
import no.naw.paw.ledigestillinger.model.Paging
import java.time.Duration

class StillingServiceV2Test : FreeSpec({
    with(TestContext.buildWithDatabase()) {

        beforeSpec {
            setUp()
        }

        afterSpec {
            tearDown()
        }

        "Skal motta stillinger fra Kafka og lagre dem i databasen og deretter søke de opp" {
            // GIVEN
            val mottatteStillinger = TestData.mottatteMessages
                .map { it.value.asDto() }
            val foersteUtloepMottatteStillinger = TestData.foersteUtloepMottatteMessages
                .map { it.value.asDto() }
            val andreUtloepMottatteStillinger = TestData.andreUtloepMottatteMessages
                .map { it.value.asDto() }
            val relevanteStillinger = TestData.relevanteMessages
                .map { it.value.asDto() }

            TestData.alleMessages shouldHaveSize 20
            mottatteStillinger shouldHaveSize 19
            foersteUtloepMottatteStillinger shouldHaveSize 17
            andreUtloepMottatteStillinger shouldHaveSize 13
            relevanteStillinger shouldHaveSize 11

            // WHEN
            stillingServiceV2.handleMessages(TestData.alleMessages.asSequence())

            // THEN
            val lagredeStillinger1 = StillingerTableV2.selectRows().map { it.asDto() }
            lagredeStillinger1 shouldHaveSize 19
            lagredeStillinger1 shouldContainOnly mottatteStillinger

            // WHEN
            val stillinger = stillingServiceV2.finnStillingerByEgenskaper(
                medSoekeord = emptyList(),
                medFylker = emptyList(),
                medStyrkkoder = emptyList(),
                paging = Paging(1, 100)
            )

            // THEN
            stillinger shouldHaveSize 11
            stillinger shouldContainOnly relevanteStillinger

            // WHEN
            val stillingerForFilter = stillingServiceV2.finnStillingerByEgenskaper(
                medSoekeord = emptyList(),
                medFylker = listOf(
                    Fylke(
                        fylkesnummer = "10",
                        kommuner = emptyList()
                    ),
                    Fylke(
                        fylkesnummer = "30",
                        kommuner = emptyList()
                    )
                ),
                medStyrkkoder = listOf("1012", "3011"),
                paging = Paging(1, 100)
            )

            // THEN
            stillingerForFilter shouldHaveSize 2
            stillingerForFilter shouldContainOnly listOf(
                TestData.message1_2.value.asDto(),
                TestData.message3_1.value.asDto()
            )

            // WHEN
            val lagredeStillinger2 = StillingerTableV2.selectRows().map { it.asDto() }

            // THEN
            lagredeStillinger2 shouldHaveSize 19
            lagredeStillinger2 shouldContainOnly mottatteStillinger

            clock.advance(Duration.ofDays(10))

            // WHEN
            stillingServiceV2.slettUtloepteStillinger()
            val lagredeStillinger3 = StillingerTableV2.selectRows().map { it.asDto() }

            // THEN
            lagredeStillinger3 shouldHaveSize 17
            lagredeStillinger3 shouldContainOnly foersteUtloepMottatteStillinger

            clock.advance(Duration.ofDays(10))

            // WHEN
            stillingServiceV2.slettUtloepteStillinger()
            val lagredeStillinger4 = StillingerTableV2.selectRows().map { it.asDto() }

            // THEN
            lagredeStillinger4 shouldHaveSize 13
            lagredeStillinger4 shouldContainOnly andreUtloepMottatteStillinger
        }
    }
})
