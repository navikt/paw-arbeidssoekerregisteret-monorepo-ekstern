package no.nav.paw.arbeidssoekerregisteret.config

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldNotBe
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

class AppConfigTest : FreeSpec({
    "Skal laste config" {
        val applicationConfig = loadNaisOrLocalConfiguration<ApplicationConfig>(APPLICATION_CONFIG_FILE_NAME)
        applicationConfig.kafka shouldNotBe null
        applicationConfig.kafkaProducer shouldNotBe null
        applicationConfig.kafkaStreams shouldNotBe null
        applicationConfig.regler shouldNotBe null
        applicationConfig.microfrontends shouldNotBe null
    }
})