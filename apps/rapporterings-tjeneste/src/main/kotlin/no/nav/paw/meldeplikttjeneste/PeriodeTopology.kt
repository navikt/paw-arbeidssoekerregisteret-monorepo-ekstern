package no.nav.paw.meldeplikttjeneste

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.config.kafka.streams.genericProcess
import no.nav.paw.config.kafka.streams.mapWithContext
import no.nav.paw.kafkakeygenerator.client.KafkaKeysResponse
import no.nav.paw.meldeplikttjeneste.tilstand.InternTilstand
import no.nav.paw.meldeplikttjeneste.tilstand.InternTilstandSerde
import no.nav.paw.meldeplikttjeneste.tilstand.initTilstand
import no.nav.paw.rapportering.internehendelser.PeriodeAvsluttet
import no.nav.paw.rapportering.internehendelser.RapporteringsHendelse
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.state.KeyValueStore
import java.util.*


context(ApplicationConfiguration, ApplicationContext)
fun StreamsBuilder.processPeriodeTopic(kafkaKeyFunction: (String) -> KafkaKeysResponse) {
    stream<Long, Periode>(periodeTopic)
        .mapWithContext("lagreEllerSlettPeriode", statStoreName) { periode ->
            val keyValueStore: KeyValueStore<Long, InternTilstand> = getStateStore(statStoreName)
            val idAndKey = kafkaKeyFunction(periode.identitetsnummer)
            val currentState = keyValueStore[idAndKey.id]
            when {
                currentState == null && periode.avsluttet() -> Action.DoNothing
                periode.avsluttet() -> Action.DeleteStateAndEmit(idAndKey.id, periode)
                currentState == null -> Action.UpdateState(initTilstand(idAndKey, periode))
                else -> Action.DoNothing
            }
        }
        .genericProcess("executeAction", statStoreName) { record ->
            val keyValueStore: KeyValueStore<Long, InternTilstand> = getStateStore(statStoreName)
            when (val action = record.value()) {
                is Action.DeleteStateAndEmit -> {
                    keyValueStore.delete(action.arbeidsoekerId)
                    forward(
                        record.withValue(
                            PeriodeAvsluttet(
                                UUID.randomUUID(),
                                action.periode.id,
                                action.periode.identitetsnummer,
                                action.arbeidsoekerId
                            ) as RapporteringsHendelse
                        )
                    )
                }

                Action.DoNothing -> {}
                is Action.UpdateState -> keyValueStore.put(action.state.periode.kafkaKeysId, action.state)
            }
        }.to(rapporteringsHendelsesloggTopic, Produced.with(Serdes.Long(), rapporteringsHendelseSerde))
}

fun Periode.avsluttet(): Boolean = avsluttet != null

sealed interface Action {
    data object DoNothing : Action
    data class DeleteStateAndEmit(val arbeidsoekerId: Long, val periode: Periode) : Action
    data class UpdateState(val state: InternTilstand) : Action
}
