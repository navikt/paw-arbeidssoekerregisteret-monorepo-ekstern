package no.nav.paw.arbeidssoekerregisteret.config

import io.micrometer.prometheus.PrometheusMeterRegistry
import no.nav.paw.rapportering.internehendelser.EksternGracePeriodeUtloept
import no.nav.paw.rapportering.internehendelser.LeveringsfristUtloept
import no.nav.paw.rapportering.internehendelser.PeriodeAvsluttet
import no.nav.paw.rapportering.internehendelser.RapporteringTilgjengelig
import no.nav.paw.rapportering.internehendelser.RapporteringsHendelse
import no.nav.paw.rapportering.internehendelser.RapporteringsMeldingMottatt
import no.nav.paw.rapportering.internehendelser.RegisterGracePeriodeUtloept
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Time
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.internals.KeyValueStoreBuilder
import org.apache.kafka.streams.state.internals.RocksDBKeyValueBytesStoreSupplier
import org.slf4j.LoggerFactory

fun buildKafkaStreams(config: Config, topology: Topology) {
    val kafkaStreams = KafkaStreams(topology, StreamsConfig(config.app.kafka.asProperties()))
    kafkaStreams.setUncaughtExceptionHandler { throwable ->
        LoggerFactory.getLogger("").error("Uventet feil", throwable) // TODO
        StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_APPLICATION
    }
}

fun StreamsBuilder.buildTopology(
    config: Config,
    meterRegistry: PrometheusMeterRegistry
): Topology {
    val (kafka) = config.app
    this
        .stream("source-topic", Consumed.with(Serdes.Long(), RapporteringsHendelseSerde()))
        .mapValues { value ->
        }.to("target-topic")
    return this.build()
}

private fun <K, V> StreamsBuilder.addPersistentStateStore(
    name: String,
    keySerde: Serde<K>,
    valueSerde: Serde<V>
): StreamsBuilder {
    return this.addStateStore(
        KeyValueStoreBuilder(
            RocksDBKeyValueBytesStoreSupplier(name, false),
            keySerde,
            valueSerde,
            Time.SYSTEM
        )
    )
}

class RapporteringsHendelseProcessor : Processor<Long, RapporteringsHendelse, Long, String> {

    override fun init(context: ProcessorContext<Long, String>) {
    }

    override fun process(record: Record<Long, RapporteringsHendelse>?) {
        when (val event = record?.value()) {
            is RapporteringTilgjengelig -> processEvent(event)
            is RapporteringsMeldingMottatt -> processEvent(event)
            is PeriodeAvsluttet -> processEvent(event)
            is LeveringsfristUtloept -> processEvent(event)
            is RegisterGracePeriodeUtloept -> processEvent(event)
            is EksternGracePeriodeUtloept -> processEvent(event)
        }
    }

    private fun processEvent(event: RapporteringTilgjengelig) {

    }

    private fun processEvent(event: RapporteringsMeldingMottatt) {

    }

    private fun processEvent(event: PeriodeAvsluttet) {

    }

    private fun processEvent(event: LeveringsfristUtloept) {

    }

    private fun processEvent(event: RegisterGracePeriodeUtloept) {

    }

    private fun processEvent(event: EksternGracePeriodeUtloept) {

    }
}