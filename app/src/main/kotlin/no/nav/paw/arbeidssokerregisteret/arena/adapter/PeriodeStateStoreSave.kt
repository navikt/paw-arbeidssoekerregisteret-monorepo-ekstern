package no.nav.paw.arbeidssokerregisteret.arena.adapter

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.utils.oppdaterTempArenaTilstandMedNyVerdi
import no.nav.paw.arbeidssokerregisteret.arena.v3.ArenaArbeidssokerregisterTilstand
import no.nav.paw.arbeidssokerregisteret.arena.v3.TempArenaArbeidssokerregisterTilstand
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.reflect.KClass

fun KStream<String, out SpecificRecord>.saveToStoreForwardIfComplete(type: KClass<out BaseStateStoreSave>, storeName: String): KStream<Long, ArenaArbeidssokerregisterTilstand> {
    val processBuilder = {
        when (type) {
            ProfileringStateStoreSave::class -> ProfileringStateStoreSave(storeName)
            OpplysningerOmArbeidssoekerStateStoreSave::class -> OpplysningerOmArbeidssoekerStateStoreSave(storeName)
            PeriodeStateStoreSave::class -> PeriodeStateStoreSave(storeName)
            else -> throw IllegalArgumentException("Ukjent type ${type.simpleName}")
        }
    }
    return process(processBuilder, Named.`as`(type.simpleName), storeName)
}

class ProfileringStateStoreSave(
    stateStoreName: String
) : BaseStateStoreSave(stateStoreName)

class OpplysningerOmArbeidssoekerStateStoreSave(
    stateStoreName: String
) : BaseStateStoreSave(stateStoreName)

class PeriodeStateStoreSave(
    stateStoreName: String
) : BaseStateStoreSave(stateStoreName)

sealed class BaseStateStoreSave(
    private val stateStoreName: String
) : Processor<String, SpecificRecord, Long, ArenaArbeidssokerregisterTilstand> {
    private var keyValueStore: KeyValueStore<String, TempArenaArbeidssokerregisterTilstand>? = null
    private var context: ProcessorContext<Long, ArenaArbeidssokerregisterTilstand>? = null
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val delayBeforeDeletingClosed = Duration.ofHours(1)

    override fun init(context: ProcessorContext<Long, ArenaArbeidssokerregisterTilstand>?) {
        super.init(context)
        this.context = context
        keyValueStore = context?.getStateStore(stateStoreName)
        requireNotNull(context) { "Context er ikke satt" }
            .schedule(Duration.ofMinutes(10L), PunctuationType.STREAM_TIME) { streamTime ->
                val stateStore =
                    context.getStateStore<KeyValueStore<String, TempArenaArbeidssokerregisterTilstand>>(stateStoreName)
                stateStore.all().use { iterator ->
                    iterator.forEach { kv ->
                        val key = kv.key
                        val temp = kv.value
                        if (temp.wasClosedBefore(streamTime - delayBeforeDeletingClosed.toMillis())) {
                            stateStore.delete(key)
                            logger.debug(
                                "Slettet nøkkel {} fra state store: avsluttet={}, streamTime={}",
                                key,
                                temp.periode.avsluttet.tidspunkt.toEpochMilli(),
                                streamTime
                            )
                        }
                    }
                }
            }
    }

    fun TempArenaArbeidssokerregisterTilstand.wasClosedBefore(time: Long): Boolean {
        val avsluttet = periode?.avsluttet?.tidspunkt
        return avsluttet != null && avsluttet.toEpochMilli() < time
    }

    override fun process(record: Record<String, SpecificRecord>?) {
        if (record == null) return
        process(
            requireNotNull(context) { "Context er ikke satt" },
            requireNotNull(keyValueStore) { "TilstandsDb er ikke initialisert" },
            record
        )
    }

    private fun process(
        ctx: ProcessorContext<Long, ArenaArbeidssokerregisterTilstand>,
        db: KeyValueStore<String, TempArenaArbeidssokerregisterTilstand>,
        record: Record<String, SpecificRecord>
    ) {
        val value = record.value()
        val temp = oppdaterTempArenaTilstandMedNyVerdi(
            nyVerdi = value,
            gjeldeneTilstand = (db.get(record.key()) ?: TempArenaArbeidssokerregisterTilstand())
        )
        if (temp.periode != null && temp.profilering != null && temp.opplysningerOmArbeidssoeker != null) {
            db.delete(record.key())
            val valueToForward = ArenaArbeidssokerregisterTilstand(
                temp.periode,
                temp.profilering,
                temp.opplysningerOmArbeidssoeker

            )
            ctx.forward(record.withValue(valueToForward).withKey(record.key().split(":").first().toLong()))
        } else {
            db.put(record.key(), temp)
        }
    }


    override fun close() {
        super.close()
        context = null
        keyValueStore = null
    }
}
