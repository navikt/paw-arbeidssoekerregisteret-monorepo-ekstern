package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.test.TestContext
import no.nav.paw.ledigestillinger.test.TestData
import no.nav.paw.ledigestillinger.test.selectRows
import no.naw.paw.ledigestillinger.model.Fylke
import no.naw.paw.ledigestillinger.model.Paging
import java.time.Duration

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
            val mottatteStillinger = TestData.mottatteMessages
                .map { it.value.asDto() }
            val foersteUtloepMottatteStillinger = TestData.foersteUtloepMottatteMessages
                .map { it.value.asDto() }
            val andreUtloepMottatteStillinger = TestData.andreUtloepMottatteMessages
                .map { it.value.asDto() }
            val relevanteStillinger = TestData.relevanteMessages
                .map { it.value.asDto() }

            mottatteStillinger shouldHaveSize 18
            foersteUtloepMottatteStillinger shouldHaveSize 17
            andreUtloepMottatteStillinger shouldHaveSize 13
            relevanteStillinger shouldHaveSize 11

            // WHEN
            stillingService.handleMessages(TestData.alleMessages.asSequence())

            // THEN
            val lagredeStillinger1 = StillingerTable.selectRows().map { it.asDto() }
            lagredeStillinger1 shouldHaveSize 18
            lagredeStillinger1 shouldContainOnly mottatteStillinger

            // WHEN
            val stillinger = stillingService.finnStillingerByEgenskaper(
                medSoekeord = emptyList(),
                medFylker = emptyList(),
                medStyrkkoder = emptyList(),
                paging = Paging(1, 100)
            )

            // THEN
            stillinger shouldHaveSize 11
            stillinger shouldContainOnly relevanteStillinger

            // WHEN
            val stillingerForFilter = stillingService.finnStillingerByEgenskaper(
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
            stillingService.slettUtloepteStillinger()
            val lagredeStillinger2 = StillingerTable.selectRows().map { it.asDto() }

            // THEN
            lagredeStillinger2 shouldHaveSize 18
            lagredeStillinger2 shouldContainOnly mottatteStillinger

            clock.advanceClock(Duration.ofDays(10))

            // WHEN
            stillingService.slettUtloepteStillinger()
            val lagredeStillinger3 = StillingerTable.selectRows().map { it.asDto() }

            // THEN
            lagredeStillinger3 shouldHaveSize 17
            lagredeStillinger3 shouldContainOnly foersteUtloepMottatteStillinger

            clock.advanceClock(Duration.ofDays(10))

            // WHEN
            stillingService.slettUtloepteStillinger()
            val lagredeStillinger4 = StillingerTable.selectRows().map { it.asDto() }

            // THEN
            lagredeStillinger4 shouldHaveSize 13
            lagredeStillinger4 shouldContainOnly andreUtloepMottatteStillinger
        }
    }
})
