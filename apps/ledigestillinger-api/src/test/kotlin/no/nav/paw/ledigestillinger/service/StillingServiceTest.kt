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
import no.naw.paw.ledigestillinger.model.StillingStatus
import java.time.Instant

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
            val sletteDato = Instant.now()
                .minus(applicationConfig.slettIkkeAktiveStillingerMedUtloeperEldreEnn)
            val messages = TestData.messages
            val alleStillinger = messages.map { it.value.asDto() }
            val forventedeStillinger = messages
                .filter { it.value.source != "DIR" }
                .map { it.value.asDto() }
                .filter { it.status == StillingStatus.AKTIV }
            val aktiveStillinger = alleStillinger.filter { it.status == StillingStatus.AKTIV }
            val ikkeAktiveOgIkkeUtloepteStillinger = alleStillinger.filter { it.status != StillingStatus.AKTIV }
                .filter { it.utloeper == null || sletteDato.isBefore(it.utloeper) }
            val beholdteStillinger = aktiveStillinger + ikkeAktiveOgIkkeUtloepteStillinger

            forventedeStillinger shouldHaveSize 12

            // WHEN
            stillingService.handleMessages(messages.asSequence())

            // THEN
            val rows = StillingerTable.selectRows()
            rows shouldHaveSize 20
            rows.map { it.asDto() } shouldContainOnly alleStillinger

            // WHEN
            val stillinger = stillingService.finnStillingerByEgenskaper(
                medSoekeord = emptyList(),
                medFylker = emptyList(),
                medStyrkkoder = emptyList(),
                paging = Paging(1, 100)
            )

            // THEN
            stillinger shouldHaveSize 12
            stillinger shouldContainOnly forventedeStillinger

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
            stillingService.slettGamleStillinger()
            val rows2 = StillingerTable.selectRows()

            // THEN
            rows2 shouldHaveSize 18
            rows2.map { it.asDto() } shouldContainOnly beholdteStillinger
        }
    }
})
