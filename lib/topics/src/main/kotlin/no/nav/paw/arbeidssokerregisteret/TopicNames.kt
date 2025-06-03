package no.nav.paw.arbeidssokerregisteret

import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration

data class TopicNames(
    val periodeTopic: String,
    val opplysningerTopic: String,
    val profileringTopic: String,
    val bekreftelseTopic: String,
    val paavnegneavTopic: String
)

val standardTopicNames: TopicNames get() = loadNaisOrLocalConfiguration("topic_names.toml")
