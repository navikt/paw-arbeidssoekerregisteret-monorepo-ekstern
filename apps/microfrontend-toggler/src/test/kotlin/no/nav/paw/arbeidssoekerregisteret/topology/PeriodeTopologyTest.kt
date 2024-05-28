package no.nav.paw.arbeidssoekerregisteret.topology

import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.paw.arbeidssoekerregisteret.model.ENABLE_ACTION
import no.nav.paw.arbeidssoekerregisteret.model.SENSITIVITET_HIGH
import no.nav.paw.arbeidssoekerregisteret.model.Toggle

@Ignored
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
                    action shouldBe ENABLE_ACTION
                    ident shouldBe periode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaMinSide
                    sensitivitet shouldBe SENSITIVITET_HIGH
                    initialedBy shouldBe "paw"
                }
                behovsvurderingKeyValue.key shouldBe kafkaKeyResponse.id
                with(minSideKeyValue.value.shouldBeInstanceOf<Toggle>()) {
                    action shouldBe ENABLE_ACTION
                    ident shouldBe periode.identitetsnummer
                    microfrontendId shouldBe appConfig.microfrontends.aiaBehovsvurdering
                    sensitivitet shouldBe SENSITIVITET_HIGH
                    initialedBy shouldBe "paw"
                }
            }
        }
    }
})