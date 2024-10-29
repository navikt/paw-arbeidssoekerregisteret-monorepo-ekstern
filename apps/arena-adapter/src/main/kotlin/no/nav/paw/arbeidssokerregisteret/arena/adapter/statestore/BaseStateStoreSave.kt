package no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore

import io.micrometer.core.instrument.Gauge.builder
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.utils.skalSlettes
import no.nav.paw.arbeidssokerregisteret.arena.adapter.utils.oppdaterTempArenaTilstandMedNyVerdi
import no.nav.paw.arbeidssokerregisteret.arena.helpers.v4.TopicsJoin
import no.nav.paw.arbeidssokerregisteret.arena.v5.ArenaArbeidssokerregisterTilstand
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.ProcessorSupplier
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

val meterIdMap = ConcurrentHashMap<Int, Pair<Meter.Id, AtomicLong>>()

class StateStoreSaveSupplier(
    private val type: KClass<out BaseStateStoreSave>,
    private val storeName: String,
    private val registry: PrometheusMeterRegistry
) : ProcessorSupplier<Long, SpecificRecord, Long, ArenaArbeidssokerregisterTilstand> {
    override fun get(): Processor<Long, SpecificRecord, Long, ArenaArbeidssokerregisterTilstand> {
        return when (type) {
            ProfileringStateStoreSave::class -> ProfileringStateStoreSave(storeName, registry)
            OpplysningerOmArbeidssoekerStateStoreSave::class -> OpplysningerOmArbeidssoekerStateStoreSave(storeName, registry)
            PeriodeStateStoreSave::class -> PeriodeStateStoreSave(storeName, registry)
            else -> throw IllegalArgumentException("Ukjent type ${type.simpleName}")
        }
    }
}

sealed class BaseStateStoreSave(
    private val stateStoreName: String,
    private val registry: PrometheusMeterRegistry
) : Processor<Long, SpecificRecord, Long, ArenaArbeidssokerregisterTilstand> {
    private var keyValueStore: KeyValueStore<UUID, TopicsJoin>? = null
    private var context: ProcessorContext<Long, ArenaArbeidssokerregisterTilstand>? = null
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun init(context: ProcessorContext<Long, ArenaArbeidssokerregisterTilstand>?) {
        super.init(context)
        this.context = context
        keyValueStore = context?.getStateStore(stateStoreName)
        requireNotNull(context) { "Context er ikke satt" }
            .schedule(Duration.ofMinutes(10L), PunctuationType.WALL_CLOCK_TIME) { streamTime ->
                val stateStore =
                    context.getStateStore<KeyValueStore<UUID, TopicsJoin>>(stateStoreName)
                val wallClock = Instant.ofEpochMilli(context.currentSystemTimeMs())
                val streamTimeInstant = Instant.ofEpochMilli(context.currentStreamTimeMs())
                val tidsfrister = kalkulerTidsfrister(
                    wallClock = wallClock,
                    streamTime = streamTimeInstant
                )
                var antallSlettede = 0
                var totaltAntall = 0
                stateStore.all().use { iterator ->
                    iterator.forEach { kv ->
                        val key = kv.key
                        val topicsJoin = kv.value
                        if (topicsJoin.skalSlettes(
                                gjeldeneTid = streamTimeInstant,
                                tidsfrister = tidsfrister
                            )
                        ) {
                            stateStore.delete(key)
                            antallSlettede += 1
                            logger.debug(
                                "Slettet nøkkel {} fra state store: avsluttet={}, opplysninger={}, streamTime={}",
                                key,
                                topicsJoin.periode?.avsluttet?.tidspunkt,
                                topicsJoin.opplysningerOmArbeidssoeker?.sendtInnAv?.tidspunkt,
                                streamTime
                            )
                        }
                        totaltAntall += 1
                    }
                }
                val taskPartition = context.taskId().partition()
                val meterId = meterIdMap[taskPartition]
                if (meterId != null) {
                    meterId.second.set(totaltAntall.toLong())
                } else {
                    val newStateObject = AtomicLong(totaltAntall.toLong())
                    val regInfo = builder("paw_arbeidssoekerregisteret_arena_adapter_joinstates_size", newStateObject, AtomicLong::toDouble)
                        .tags(Tags.of(Tag.of("partition", taskPartition.toString())))
                        .register(registry)
                    meterIdMap[taskPartition] = Pair(regInfo.id, newStateObject)
                }

                logger.info(
                    "Slettet {} nøkler fra state store, stream-time: {}, wall-clock: {}, frister: {}",
                    antallSlettede,
                    streamTimeInstant,
                    wallClock,
                    tidsfrister
                )
            }
    }


    override fun process(record: Record<Long, SpecificRecord>?) {
        if (record == null) return
        process(
            requireNotNull(context) { "Context er ikke satt" },
            requireNotNull(keyValueStore) { "TilstandsDb er ikke initialisert" },
            record
        )
    }

    private fun process(
        ctx: ProcessorContext<Long, ArenaArbeidssokerregisterTilstand>,
        db: KeyValueStore<UUID, TopicsJoin>,
        record: Record<Long, SpecificRecord>
    ) {
        val value = record.value()
        val key = record.value().periodeId()
        val existingValue = db.get(key)
        val temp = oppdaterTempArenaTilstandMedNyVerdi(
            nyVerdi = value,
            gjeldeneTilstand = (existingValue ?: TopicsJoin())
        )
        if (temp == existingValue) {
            return
        }
        if (isComplete(temp)) {
            db.delete(key)
            val valueToForward = ArenaArbeidssokerregisterTilstand(
                temp.periode,
                temp.profilering,
                temp.opplysningerOmArbeidssoeker
            )
            ctx.forward(record.withValue(valueToForward))
        } else {
            db.put(key, temp)
        }
    }

    private fun isComplete(temp: TopicsJoin) =
        (temp.periode != null && temp.periode.avsluttet != null ) ||
                (temp.periode != null && temp.profilering != null && temp.opplysningerOmArbeidssoeker != null)


    fun SpecificRecord.periodeId(): UUID {
        return when (this) {
            is Periode -> id
            is Profilering -> periodeId
            is OpplysningerOmArbeidssoeker -> periodeId
            else -> throw IllegalArgumentException("Ukjent type ${this::class.simpleName}")
        }
    }

    override fun close() {
        super.close()
        context = null
        keyValueStore = null
    }
}

class OpplysningerOmArbeidssoekerStateStoreSave(
    stateStoreName: String,
    registry: PrometheusMeterRegistry
) : BaseStateStoreSave(stateStoreName, registry)

class PeriodeStateStoreSave(
    stateStoreName: String,
    registry: PrometheusMeterRegistry
) : BaseStateStoreSave(stateStoreName, registry)

class ProfileringStateStoreSave(
    stateStoreName: String,
    registry: PrometheusMeterRegistry
) : BaseStateStoreSave(stateStoreName, registry)
