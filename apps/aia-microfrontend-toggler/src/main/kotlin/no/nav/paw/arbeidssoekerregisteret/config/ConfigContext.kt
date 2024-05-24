package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.config.kafka.KafkaConfig

data class ConfigContext(val appConfig: AppConfig, val kafkaConfig: KafkaConfig)
