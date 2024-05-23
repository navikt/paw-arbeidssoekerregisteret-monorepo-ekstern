package no.nav.paw.meldeplikttjeneste

import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.rapportering.ansvar.v1.AnsvarEndret
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

context(ApplicationConfiguration, ApplicationContext)
fun StreamsBuilder.topology(
    kafkaKeyFunction: (String) -> KafkaKeysResponse
): Topology {
    processPeriodeTopic(kafkaKeyFunction)
    processAnsvarTopic()
    processRapporteringsMeldingTopic()

    return build()
}
