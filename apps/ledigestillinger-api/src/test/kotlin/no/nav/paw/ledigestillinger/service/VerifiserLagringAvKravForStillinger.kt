package no.nav.paw.ledigestillinger.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.model.asDto
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.test.TestContext
import no.nav.paw.ledigestillinger.test.TestData
import no.nav.paw.ledigestillinger.test.TestData.eksperimentelleProperties
import no.nav.paw.ledigestillinger.test.selectRows
import no.naw.paw.ledigestillinger.model.Tag
import no.naw.paw.ledigestillinger.model.TekniskTag
import java.util.*

class VerifiserLagringAvKravForStillinger : FreeSpec({
    with(TestContext.buildWithDatabase()) {

        beforeSpec {
            setUp()
        }

        afterSpec {
            tearDown()
        }

        "Skal tolke og lagre stillinger med krav" {
            // GIVEN
            val message1: Message<UUID, Ad> = TestData.message(
                eksperimentelleProperties = eksperimentelleProperties(
                    experience = setOf("Ingen"),
                    education = setOf("Ingen krav"),
                    needDriversLicense = "false"
                )
            )
            val message2: Message<UUID, Ad> = TestData.message(
                eksperimentelleProperties = eksperimentelleProperties(
                    experience = setOf("Noe"),
                    needDriversLicense = "   true   "
                )
            )
            val message3: Message<UUID, Ad> = TestData.message(
                eksperimentelleProperties = eksperimentelleProperties(
                    experience = setOf("Mye"),
                    education = setOf("Fagbrev", "Fagskole"),
                    needDriversLicense = "true"
                )
            )
            val message4: Message<UUID, Ad> = TestData.message(
                eksperimentelleProperties = eksperimentelleProperties(
                    education = setOf("Master", "Forskningsgrad"),
                    needDriversLicense = "Whatever"
                )
            )
            val message5: Message<UUID, Ad> = TestData.message(
                eksperimentelleProperties = eksperimentelleProperties(
                    experience = setOf("Whatever")
                )
            )
            val message6: Message<UUID, Ad> = TestData.message(
                eksperimentelleProperties = eksperimentelleProperties(
                    needDriversLicense = "true"
                )
            )
            val messages = listOf(
                message1,
                message2,
                message3,
                message4,
                message5,
                message6
            )

            // WHEN
            stillingService.handleMessages(messages.asSequence())

            // THEN
            val lagredeStillinger = StillingerTable.selectRows().map { it.asDto() }
            lagredeStillinger shouldHaveSize 6
            val stilling1 = lagredeStillinger[0]
            val stilling2 = lagredeStillinger[1]
            val stilling3 = lagredeStillinger[2]
            val stilling4 = lagredeStillinger[3]
            val stilling5 = lagredeStillinger[4]
            val stilling6 = lagredeStillinger[5]

            stilling1.tags shouldContainOnly listOf(
                Tag.INGEN_KRAV_TIL_ARBEIDSERFARING_V1,
                Tag.INGEN_KRAV_TIL_UTDANNING_V1,
                Tag.INGEN_KRAV_TIL_FOERERKORT_V1
            )
            stilling1.tekniskeTags shouldBe emptyList()

            stilling2.tags shouldContainOnly listOf(
                Tag.HAR_KRAV_TIL_ARBEIDSERFARING_V1,
                Tag.HAR_KRAV_TIL_FOERERKORT_V1
            )
            stilling2.tekniskeTags shouldBe emptyList()

            stilling3.tags shouldContainOnly listOf(
                Tag.HAR_KRAV_TIL_ARBEIDSERFARING_V1,
                Tag.HAR_KRAV_TIL_UTDANNING_V1,
                Tag.HAR_KRAV_TIL_FOERERKORT_V1
            )
            stilling3.tekniskeTags shouldBe emptyList()

            stilling4.tags shouldContainOnly listOf(
                Tag.HAR_KRAV_TIL_UTDANNING_V1,
            )
            stilling4.tekniskeTags shouldContainOnly listOf(
                TekniskTag.UKJENT_KRAV_TIL_FOERERKORT_V1
            )

            stilling5.tags shouldBe emptyList()
            stilling5.tekniskeTags shouldContainOnly listOf(
                TekniskTag.UKJENT_KRAV_TIL_ARBEIDSERFARING_V1
            )

            stilling6.tags shouldContainOnly listOf(
                Tag.HAR_KRAV_TIL_FOERERKORT_V1
            )
            stilling6.tekniskeTags shouldBe emptyList()
        }
    }
})