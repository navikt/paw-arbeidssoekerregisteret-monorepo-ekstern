package no.nav.paw.arbeidssoekerregisteret.context

import no.nav.paw.arbeidssoekerregisteret.config.AppConfig
import no.nav.paw.config.kafka.KafkaConfig

data class ConfigContext(val appConfig: AppConfig, val kafkaConfig: KafkaConfig)
