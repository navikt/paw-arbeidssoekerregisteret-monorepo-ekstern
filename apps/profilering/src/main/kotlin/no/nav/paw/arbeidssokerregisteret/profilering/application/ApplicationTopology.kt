package no.nav.paw.arbeidssokerregisteret.profilering.application

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfoTjeneste
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology

fun applicationTopology(
    streamBuilder: StreamsBuilder,
    personInfoTjeneste: PersonInfoTjeneste,
    applicationConfiguration: ApplicationConfiguration,
    prometheusRegistry: PrometheusMeterRegistry
): Topology {
    val setupStreams = SetupStreams(streamBuilder, personInfoTjeneste, applicationConfiguration, prometheusRegistry)
    setupStreams.setupStream<Periode>(
        applicationConfiguration.periodeTopic,
        PeriodeStateStoreSave::class
    )
    setupStreams.setupStream<OpplysningerOmArbeidssoeker>(
        applicationConfiguration.opplysningerTopic,
        OpplysningerOmArbeidssoekerStateStoreSave::class
    )
    return streamBuilder.build()
}

