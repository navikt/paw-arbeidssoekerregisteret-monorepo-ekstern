package no.nav.paw.arbeidssoekerregisteret.config

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.config.env.NaisEnv
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

class AppConfigTest : FreeSpec({
    val appConfig = loadNaisOrLocalConfiguration<AppConfig>(APPLICATION_CONFIG_FILE_NAME)

    "Skal ha feature toggles satt" {
        appConfig.featureToggles.isKafkaStreamsEnabled(NaisEnv.Local) shouldBe true
        appConfig.featureToggles.isKafkaStreamsEnabled(NaisEnv.DevGCP) shouldBe false
        appConfig.featureToggles.isKafkaStreamsEnabled(NaisEnv.ProdGCP) shouldBe false

        appConfig.featureToggles.isPeriodeTopologyEnabled(NaisEnv.Local) shouldBe true
        appConfig.featureToggles.isPeriodeTopologyEnabled(NaisEnv.DevGCP) shouldBe false
        appConfig.featureToggles.isPeriodeTopologyEnabled(NaisEnv.ProdGCP) shouldBe false

        appConfig.featureToggles.is14aVedtakTopologyEnabled(NaisEnv.Local) shouldBe true
        appConfig.featureToggles.is14aVedtakTopologyEnabled(NaisEnv.DevGCP) shouldBe false
        appConfig.featureToggles.is14aVedtakTopologyEnabled(NaisEnv.ProdGCP) shouldBe false
    }
})