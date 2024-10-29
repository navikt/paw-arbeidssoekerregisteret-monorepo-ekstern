package no.nav.paw.arbeidssokerregisteret.arena.adapter

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.config.Topics
import no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore.OpplysningerOmArbeidssoekerStateStoreSave
import no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore.PeriodeStateStoreSave
import no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore.ProfileringStateStoreSave
import no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore.saveToStoreForwardIfComplete
import no.nav.paw.arbeidssokerregisteret.arena.adapter.utils.filter
import no.nav.paw.arbeidssokerregisteret.arena.v5.ArenaArbeidssokerregisterTilstand
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

val HOEYVANNSMERKE = Instant.parse("2024-01-01T00:00:00Z")
private val uteAvSyncLogger = LoggerFactory.getLogger("ute_av_sync")

fun topology(
    builder: StreamsBuilder,
    topics: Topics,
    stateStoreName: String,
    periodeSerde: Serde<Periode>,
    opplysningerOmArbeidssoekerSerde: Serde<OpplysningerOmArbeidssoeker>,
    profileringSerde: Serde<Profilering>,
    arenaArbeidssokerregisterTilstandSerde: Serde<ArenaArbeidssokerregisterTilstand>,
    registry: PrometheusMeterRegistry
): Topology {
    builder.stream(
        topics.arbeidssokerperioder,
        Consumed.with(Serdes.Long(), periodeSerde)
    ).filter { _, periode ->
        periode.startet.tidspunkt.isAfter(HOEYVANNSMERKE) ||
                (periode.avsluttet != null && periode.avsluttet.tidspunkt.isAfter(HOEYVANNSMERKE))
    }.saveToStoreForwardIfComplete(
        type = PeriodeStateStoreSave::class,
        storeName = stateStoreName,
        registry = registry
    ).to(
        topics.arena,
        Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde)
    )

    builder.stream(
        topics.opplysningerOmArbeidssoeker,
        Consumed.with(Serdes.Long(), opplysningerOmArbeidssoekerSerde)
    ).filter { _, opplysninger ->
        opplysninger.sendtInnAv.tidspunkt.isAfter(HOEYVANNSMERKE)
    }.saveToStoreForwardIfComplete(
        type = OpplysningerOmArbeidssoekerStateStoreSave::class,
        storeName = stateStoreName,
        registry = registry
    ).to(
        topics.arena,
        Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde)
    )

    builder.stream(
        topics.profilering,
        Consumed.with(Serdes.Long(), profileringSerde)
    ).filter("filterOnRecordTimestamp") { record ->
        Instant.ofEpochMilli(record.timestamp()).isAfter(HOEYVANNSMERKE)
    }.saveToStoreForwardIfComplete(
        type = ProfileringStateStoreSave::class,
        storeName = stateStoreName,
        registry = registry
    )
        .assertCompleteMessage()
        .logInvalidMessages()
        .to(
            topics.arena,
            Produced.with(Serdes.Long(), arenaArbeidssokerregisterTilstandSerde),
        )
    return builder.build()
}

fun KStream<Long, ArenaArbeidssokerregisterTilstand>.assertCompleteMessage(): KStream<Long, ArenaArbeidssokerregisterTilstand> =
    peek { _, arenaTilstand ->
        requireNotNull(arenaTilstand.periode) { "Periode mangler" }
        requireNotNull(arenaTilstand.opplysningerOmArbeidssoeker) { "Opplysninger om arbeidssøker mangler" }
        requireNotNull(arenaTilstand.profilering) { "Profilering mangler" }
    }

fun KStream<Long, ArenaArbeidssokerregisterTilstand>.logInvalidMessages(): KStream<Long, ArenaArbeidssokerregisterTilstand> =
    peek { _, arenaTilstand ->
        runCatching {
            requireNotNull(arenaTilstand.periode) { "Periode mangler" }
            require(arenaTilstand.profilering?.periodeId == null || arenaTilstand.periode.id == arenaTilstand.profilering.periodeId) { "PeriodeId (profilering) matcher ikke" }
            require(arenaTilstand.opplysningerOmArbeidssoeker?.periodeId == null || arenaTilstand.periode.id == arenaTilstand.opplysningerOmArbeidssoeker.periodeId) { "PeriodeId (opplysninger om arbeidssøker) matcher ikke" }

            require(
                listOfNotNull(
                    arenaTilstand.opplysningerOmArbeidssoeker?.id,
                    arenaTilstand.profilering?.opplysningerOmArbeidssokerId
                ).distinct().size < 2
            ) { "Opplysninger om arbeidssøkerId (profilering) matcher ikke: ${debug(arenaTilstand)}" }
        }.onFailure {
            uteAvSyncLogger.error("Ute av sync: ${it.message}")
        }
    }

fun debug(arenaTilstand: ArenaArbeidssokerregisterTilstand): DebugObject {
    return DebugObject(
        Triple(arenaTilstand.periode?.id, arenaTilstand.opplysningerOmArbeidssoeker?.periodeId, arenaTilstand.profilering.periodeId),
        arenaTilstand.opplysningerOmArbeidssoeker?.id to arenaTilstand.profilering.opplysningerOmArbeidssokerId,
        arenaTilstand.profilering?.id
    )
}

data class DebugObject(
    val periodeId: Triple<UUID?, UUID?, UUID?>,
    val opplysningId: Pair<UUID?,  UUID?>,
    val profileringsId: UUID?
)
