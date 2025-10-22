package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
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

        "Skal motta stillinger fra kafka og lagre dem i databasen" {
            // GIVEN
            val message = TestData.message()

            // WHEN
            stillingService.handleMessages(listOf(message).asSequence())

            // THEN
            val rows = StillingerTable.selectRows()
            rows shouldHaveSize 1
            val row = rows[0]
            row.uuid shouldBe message.key
            row shouldBeEqualTo message.value
        }
    }
})
