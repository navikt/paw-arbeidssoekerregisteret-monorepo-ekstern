package no.nav.paw.arbeidssoekerregisteret.config

import no.nav.paw.arbeidssoekerregisteret.model.PeriodeInfo
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleState
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore


context(ConfigContext, LoggingContext)
fun StreamsBuilder.processPeriodeTopic(kafkaKeyFunction: (String) -> KafkaKeysResponse) {
    val (kafkaTopology) = appConfig

    // TODO Legg til punctuator for håndtering av utsatt deaktivering av microfrontends

    this.stream<Long, Periode>(kafkaTopology.periodeTopic)
        .mapValues { periode ->
            PeriodeInfo(
                id = periode.id,
                identitetsnummer = periode.identitetsnummer,
                arbeidssoekerId = kafkaKeyFunction(periode.identitetsnummer).id,
                startet = periode.startet.tidspunkt,
                avsluttet = periode.avsluttet.tidspunkt
            )
        }
        .genericProcess<Long, PeriodeInfo, Long, Toggle>("prosesserPeriode", kafkaTopology.toggleStoreName) { record ->
            val keyValueStore: KeyValueStore<Long, ToggleState> = getStateStore(kafkaTopology.toggleStoreName)
            val periode = record.value()
            when {
                periode.erAvsluttet() -> {
                    keyValueStore.put(periode.arbeidssoekerId, ToggleState(periode = periode))
                }

                else -> {
                    keyValueStore.delete(periode.arbeidssoekerId)
                    forward(
                        record.withValue(
                            Toggle(
                                action = "enable",
                                ident = periode.identitetsnummer,
                                microfrontendId = "aia-min-side",
                                sensitivitet = "high",
                                initialedBy = "paw" // TODO Bruke miljøvariabel
                            )
                        )
                    )
                    forward(
                        record.withValue(
                            Toggle(
                                action = "enable",
                                ident = periode.identitetsnummer,
                                microfrontendId = "aia-behovsvurdering",
                                sensitivitet = "high",
                                initialedBy = "paw" // TODO Bruke miljøvariabel
                            )
                        )
                    )
                }
            }
        }
        .to(kafkaTopology.microfrontendTopic, Produced.with(Serdes.Long(), ToggleSerde()))
}

fun PeriodeInfo.erAvsluttet(): Boolean = avsluttet != null
