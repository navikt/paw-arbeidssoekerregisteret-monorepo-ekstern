package no.nav.paw.rapportering.api.kafka

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.rapportering.internehendelser.RapporteringsHendelseSerde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed

fun StreamsBuilder.appTopology(
    prometheusRegistry: PrometheusMeterRegistry,
    rapporteringHendelseLoggTopic: String,
    stateStoreName: String,
): Topology {
    stream(rapporteringHendelseLoggTopic, Consumed.with(Serdes.Long(), RapporteringsHendelseSerde()))
        .oppdaterRapporteringHendelseState(stateStoreName)

    return build()
}