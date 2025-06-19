package no.nav.paw.arbeidssoekerregisteret.plugins


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.context.ApplicationContext
import no.nav.paw.kafka.plugin.KafkaProducerPlugin

fun Application.configureKafka(applicationContext: ApplicationContext) {
    with(applicationContext) {
        install(KafkaProducerPlugin) {
            listOf(applicationContext.producer)
        }
    }
}
