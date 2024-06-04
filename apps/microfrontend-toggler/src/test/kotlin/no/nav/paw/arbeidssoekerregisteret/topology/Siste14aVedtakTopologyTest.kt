package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.buildPeriodeInfo
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.pdl.graphql.generated.enums.IdentGruppe
import no.nav.paw.pdl.graphql.generated.hentidenter.IdentInformasjon
import java.time.Duration
import java.time.Instant
import java.util.*

class Siste14aVedtakTopologyTest : FreeSpec({

    with(Siste14aVedtakTopologyTestContext()) {
        "Testsuite for toggling av AIA-microfrontends basert på siste 14a vedtak" - {
            val identitetsnummer = "01017012345"
            val arbeidsoekerId = 1234L
            val aktorId = "12345"
            val periodeAvsluttetTidspunkt = Instant.now()
            val periodeStartTidspunkt = periodeAvsluttetTidspunkt.minus(Duration.ofDays(10))
            val startetPeriode = buildPeriode(identitetsnummer = identitetsnummer, startet = periodeStartTidspunkt)
            val avsluttetPeriode = buildPeriode(
                identitetsnummer = identitetsnummer,
                startet = periodeStartTidspunkt,
                avsluttet = periodeAvsluttetTidspunkt
            )
            val siste14aVedtak = buildSiste14aVedtak(aktorId, periodeStartTidspunkt.plus(Duration.ofDays(2)))
            every { pdlClientMock.hentFolkeregisterIdent(aktorId) } returns IdentInformasjon(
                identitetsnummer, IdentGruppe.FOLKEREGISTERIDENT
            )
            every { kafkaKeysClientMock.hentKafkaKeys(identitetsnummer) } returns KafkaKeysResponse(
                arbeidsoekerId,
                9876
            )

            println(testDriver.producedTopicNames())

            "Skal ikke deaktivere aia-behovsvurdering microfrontend om det ikke finnes noen periode tilhørende 14a vedtak" {
                siste14aVedtakTopic.pipeInput(UUID.randomUUID().toString(), siste14aVedtak)

                microfrontendTopic.isEmpty shouldBe true
            }

            "Skal ikke deaktivere aia-behovsvurdering microfrontend om det ikke finnes en aktiv periode tilhørende 14a vedtak" {
                periodeKeyValueStore.put(arbeidsoekerId, avsluttetPeriode.buildPeriodeInfo(arbeidsoekerId))

                siste14aVedtakTopic.pipeInput(UUID.randomUUID().toString(), siste14aVedtak)

                microfrontendTopic.isEmpty shouldBe true
            }

            "Skal deaktivere aia-behovsvurdering microfrontend om det finnes en aktiv periode tilhørende 14a vedtak" {
                periodeKeyValueStore.put(arbeidsoekerId, startetPeriode.buildPeriodeInfo(arbeidsoekerId))

                siste14aVedtakTopic.pipeInput(UUID.randomUUID().toString(), siste14aVedtak)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1

                val behovsvurderingKeyValue = keyValueList.last()

                behovsvurderingKeyValue.key shouldBe arbeidsoekerId
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe avsluttetPeriode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }
            }
        }
    }
})