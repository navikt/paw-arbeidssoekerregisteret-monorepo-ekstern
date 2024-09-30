package no.nav.paw.arbeidssoekerregisteret.plugins


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext

fun Application.configureKafka(applicationContext: ApplicationContext) {
    with(applicationContext) {
        install(KafkaStreamsPlugin) {
            shutDownTimeout = applicationConfig.kafkaStreams.shutDownTimeout
            kafkaStreams = listOf(periodeKafkaStreams, siste14aVedtakKafkaStreams)
        }
    }
}
