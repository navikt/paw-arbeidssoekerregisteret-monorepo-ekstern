package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.model.Sensitivitet
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import java.time.Duration
import java.time.Instant

class PeriodeTopologyTest : FreeSpec({

    with(TopologyTestContext()) {
        "Testsuite for toggling av AIA-microfrontends basert på arbeidssøkerperiode" - {
            val identitetsnummer = "01017012345"
            val periodeAvsluttetTidspunkt = Instant.now()
            val periodeStartTidspunkt = periodeAvsluttetTidspunkt.minus(Duration.ofDays(10))
            val startetPeriode = buildPeriode(identitetsnummer = identitetsnummer, startet = periodeStartTidspunkt)
            val avsluttetPeriode = buildPeriode(
                identitetsnummer = identitetsnummer,
                startet = periodeStartTidspunkt,
                avsluttet = periodeAvsluttetTidspunkt
            )


            "Skal aktivere nødvendige microfrontends ved start av periode" {
                val (periode, kafkaKeyResponse) = startetPeriode
                periodeTopic.pipeInput(kafkaKeyResponse.key, periode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 2
                val minSideKeyValue = keyValueList.first()
                val behovsvurderingKeyValue = keyValueList.last()
                minSideKeyValue.key shouldBe kafkaKeyResponse.id
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe periode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }
                behovsvurderingKeyValue.key shouldBe kafkaKeyResponse.id
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.ENABLE
                    ident shouldBe periode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe Sensitivitet.HIGH
                    initialedBy shouldBe "paw"
                }
            }

            "Skal deaktivere nødvendige microfrontends ved avslutting av periode" {
                val (periode, kafkaKeyResponse) = avsluttetPeriode
                periodeTopic.pipeInput(kafkaKeyResponse.key, periode)

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1
                val behovsvurderingKeyValue = keyValueList.last()
                behovsvurderingKeyValue.key shouldBe kafkaKeyResponse.id
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe periode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }
            }

            "Skal deaktivere nødvendige microfrontends 21 dager etter avslutting av periode" {
                val (periode, kafkaKeyResponse) = avsluttetPeriode

                testDriver.advanceWallClockTime(Duration.ofDays(22))

                microfrontendTopic.isEmpty shouldBe false
                val keyValueList = microfrontendTopic.readKeyValuesToList()
                keyValueList.size shouldBe 1
                val behovsvurderingKeyValue = keyValueList.last()
                behovsvurderingKeyValue.key shouldBe kafkaKeyResponse.id
                with(behovsvurderingKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ToggleAction.DISABLE
                    ident shouldBe periode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe null
                    initialedBy shouldBe "paw"
                }
            }
        }
    }
})