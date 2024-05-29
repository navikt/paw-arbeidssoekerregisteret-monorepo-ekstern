package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.model.Sensitivitet
import no.nav.paw.arbeidssoekerregisteret.model.Toggle

class PeriodeTopologyTest : FreeSpec({

    with(TopologyTestContext()) {
        "Testsuite for toggling av AIA-microfrontends basert på arbeidssøkerperiode" - {
            val (periode, kafkaKeyResponse) = buildPeriode(identitetsnummer = "12345678901")
            periodeTopic.pipeInput(kafkaKeyResponse.key, periode)

            "Ved start av periode så aktiveres nødvendige microfrontends" {
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
        }
    }
})