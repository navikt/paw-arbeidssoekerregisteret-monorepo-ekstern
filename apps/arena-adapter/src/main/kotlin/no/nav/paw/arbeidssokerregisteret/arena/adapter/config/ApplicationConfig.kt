package no.nav.paw.arbeidssokerregisteret.arena.adapter.config

import no.nav.paw.arbeidssokerregisteret.TopicNames

data class ApplicationConfig(
    val applicationIdSuffix: String,
    val topics: ArenaAdapterTopics
)

data class ArenaAdapterTopics(
    val arena: String
)

class Topics(
    arenaTopics: ArenaAdapterTopics,
    standardTopics: TopicNames
) {
    val opplysningerOmArbeidssoeker: String = standardTopics.opplysningerTopic
    val arbeidssokerperioder: String = standardTopics.periodeTopic
    val profilering: String = standardTopics.profileringTopic
    val arena: String = arenaTopics.arena
    val bekreftelse: String = standardTopics.bekreftelseTopic
}