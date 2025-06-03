package no.nav.paw.oppslagapi

import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.config.KafkaConfig

fun main() {
    val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val kafkaConfig = loadNaisOrLocalConfiguration<KafkaConfig>(KAFKA_CONFIG_WITH_SCHEME_REG)
    val topicNames = standardTopicNames

    embeddedServer(Netty, port = 8080) {
        routing {
            route("internal") {
                route("isAlive") {
                    get {
                        call.respondText("OK", status = HttpStatusCode.OK)
                    }
                }
                route("isReady") {
                    get {
                        call.respondText("OK", status = HttpStatusCode.OK)
                    }
                }
                route("isStarted") {
                    get {
                        call.respondText("OK", status = HttpStatusCode.OK)
                    }
                }
                route("metrics") {
                    get {
                        call.respondText(prometheusRegistry.scrape())
                    }
                }
            }
        }
    }.start(wait = true)
}