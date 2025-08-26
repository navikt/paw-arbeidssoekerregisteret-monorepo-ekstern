package no.nav.paw.oppslagapi

import no.nav.paw.arbeidssokerregisteret.TopicNames
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig

data class Configurations(
    val kafkaConfig: KafkaConfig = loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG),
    val securityConfig: SecurityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG),
    val topicNames: TopicNames
)

fun configurations(): Configurations = Configurations(
    kafkaConfig = loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG),
    securityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG),
    topicNames = standardTopicNames(currentRuntimeEnvironment)
)