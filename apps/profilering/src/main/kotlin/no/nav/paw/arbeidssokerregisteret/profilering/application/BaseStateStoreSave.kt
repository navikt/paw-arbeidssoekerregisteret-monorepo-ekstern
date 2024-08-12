package no.nav.paw.arbeidssokerregisteret.profilering.application

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.helpers.v4.TopicsJoin
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KClass

fun KStream<Long, TopicsJoin>.saveAndForwardIfComplete(
    type: KClass<out BaseStateStoreSave>,
    stateStoreName: String,
    prometheusMeterRegistry: PrometheusMeterRegistry
): KStream<Long, TopicsJoin> {
    val processor = {
        type.java.getDeclaredConstructor(String::class.java, PrometheusMeterRegistry::class.java)
            .newInstance(stateStoreName, prometheusMeterRegistry)
    }
    return process(processor, Named.`as`(type.simpleName), stateStoreName)
}

private val metricsMap = ConcurrentHashMap<Int, AtomicLong>()
sealed class BaseStateStoreSave(
    private val stateStoreName: String,
    private val prometheusMeterRegistry: PrometheusMeterRegistry
) : Processor<Long, TopicsJoin, Long, TopicsJoin> {
    private var stateStore: KeyValueStore<UUID, TopicsJoin>? = null
    private var context: ProcessorContext<Long, TopicsJoin>? = null
    private val logger = LoggerFactory.getLogger("applicationTopology")

    override fun init(context: ProcessorContext<Long, TopicsJoin>?) {
        super.init(context)
        this.context = context
        stateStore = context?.getStateStore(stateStoreName)
        val gaugeValue = metricsMap.computeIfAbsent(context?.taskId()?.partition() ?: 0) { _ -> AtomicLong(0) }
        prometheusMeterRegistry.gauge(
            METRICS_STATE_STORE_ELEMENTS,
            Tags.of(
                Tag.of(LABEL_STATE_STORE_NAME, stateStoreName),
                Tag.of(LABEL_STATE_STORE_PARTITION, context?.taskId()?.partition().toString())
            ),
            gaugeValue
        )
        scheduleCleanup(
            requireNotNull(context) { "Context is not initialized" },
            requireNotNull(stateStore) { "State store is not initialized" }
        )
    }

    private fun scheduleCleanup(
        ctx: ProcessorContext<Long, TopicsJoin>,
        stateStore: KeyValueStore<UUID, TopicsJoin>,
        interval: Duration = Duration.ofMinutes(10)
    ) = ctx.schedule(interval, PunctuationType.STREAM_TIME) { time ->
        try {
            val currentTime = Instant.ofEpochMilli(time)
            var valuesInStore = 0L
            stateStore.all().forEachRemaining { keyValue ->
                valuesInStore += 1
                val value = keyValue.value
                if (value.isOutdated(currentTime)) {
                    logger.debug("Sletter utdatert record med key: {}", keyValue.key)
                    stateStore.delete(keyValue.key)
                }
            }
            metricsMap[ctx.taskId().partition()]?.set(valuesInStore)
        } catch (e: Exception) {
            //Exception her ga bare "Exception caught while punctuating processor 'PeriodeStateStoreSave'"
            // i exception handler (in trace eller ekstra info om hva som gikk galt)
            logger.error("Feil ved opprydding av state store: $e", e)
            throw e
        }
    }

    override fun process(record: Record<Long, TopicsJoin>?) {
        if (record == null) return
        val store = requireNotNull(stateStore) { "State store is not initialized" }
        val ctx = requireNotNull(context) { "Context is not initialized" }
        val storeKey = record.value().periodeId()
        val currentValue = store.get(storeKey)
        val newValue = record.value() mergeTo currentValue
        if (newValue.isComplete()) {
            ctx.forward(record.withValue(newValue))
            // Vi kan få flere opplysninger på samme periode, så vi beholder den.
            store.put(storeKey, TopicsJoin(newValue.periode, null, null))
        } else {
            store.put(storeKey, newValue)
        }
    }
}

class OpplysningerOmArbeidssoekerStateStoreSave(
    stateStoreName: String,
    prometheusMeterRegistry: PrometheusMeterRegistry
) : BaseStateStoreSave(stateStoreName, prometheusMeterRegistry)

class PeriodeStateStoreSave(
    stateStoreName: String,
    prometheusMeterRegistry: PrometheusMeterRegistry
) : BaseStateStoreSave(stateStoreName, prometheusMeterRegistry)