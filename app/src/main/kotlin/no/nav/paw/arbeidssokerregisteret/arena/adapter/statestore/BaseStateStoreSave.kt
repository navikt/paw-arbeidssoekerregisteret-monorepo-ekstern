package no.nav.paw.arbeidssokerregisteret.arena.adapter.statestore

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.arena.adapter.compoundKey
import no.nav.paw.arbeidssokerregisteret.arena.adapter.utils.oppdaterTempArenaTilstandMedNyVerdi
import no.nav.paw.arbeidssokerregisteret.arena.v3.ArenaArbeidssokerregisterTilstand
import no.nav.paw.arbeidssokerregisteret.arena.v3.TempArenaArbeidssokerregisterTilstand
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

sealed class BaseStateStoreSave(
    private val stateStoreName: String
) : Processor<Long, SpecificRecord, Long, ArenaArbeidssokerregisterTilstand> {
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
        db: KeyValueStore<String, TempArenaArbeidssokerregisterTilstand>,
        record: Record<Long, SpecificRecord>
    ) {
        val value = record.value()
        val compoundKey = compoundKey(record.key(), record.value().periodeId())
        val temp = oppdaterTempArenaTilstandMedNyVerdi(
            nyVerdi = value,
            gjeldeneTilstand = (db.get(compoundKey) ?: TempArenaArbeidssokerregisterTilstand())
        )
        if (temp.periode != null && temp.profilering != null && temp.opplysningerOmArbeidssoeker != null) {
            db.delete(compoundKey)
            val valueToForward = ArenaArbeidssokerregisterTilstand(
                temp.periode,
                temp.profilering,
                temp.opplysningerOmArbeidssoeker

            )
            ctx.forward(record.withValue(valueToForward))
        } else {
            db.put(compoundKey, temp)
        }
    }

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
    stateStoreName: String
) : BaseStateStoreSave(stateStoreName)

class PeriodeStateStoreSave(
    stateStoreName: String
) : BaseStateStoreSave(stateStoreName)

class ProfileringStateStoreSave(
    stateStoreName: String
) : BaseStateStoreSave(stateStoreName)