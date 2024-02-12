package no.nav.paw.arbeidssokerregisteret.arena.adapter

import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.arena.adapter.utils.info
import no.nav.paw.arbeidssokerregisteret.arena.adapter.utils.isValid
import no.nav.paw.arbeidssokerregisteret.arena.v3.ArenaArbeidssokerregisterTilstand
import no.nav.paw.arbeidssokerregisteret.arena.v3.PeriodeListe
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Named
import org.apache.kafka.streams.processor.api.Processor
import org.apache.kafka.streams.processor.api.ProcessorContext
import org.apache.kafka.streams.processor.api.Record
import org.apache.kafka.streams.state.KeyValueStore
import org.slf4j.LoggerFactory

fun KStream<Long, Pair<OpplysningerOmArbeidssoeker, Profilering>>.loadAndMap(storeName: String): KStream<Long, ArenaArbeidssokerregisterTilstand> {
    val processBuilder = { PeriodeStateStoreLoadAndMap(storeName) }
    return process(processBuilder, Named.`as`("periodeStateStoreLoadAndMap"), storeName)
}


class PeriodeStateStoreLoadAndMap(
    private val stateStoreName: String
) : Processor<Long, Pair<OpplysningerOmArbeidssoeker, Profilering>, Long, ArenaArbeidssokerregisterTilstand> {
    private var keyValueStore: KeyValueStore<Long, PeriodeListe>? = null
    private var context: ProcessorContext<Long, ArenaArbeidssokerregisterTilstand>? = null
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val joinErrorLogger = LoggerFactory.getLogger("join_error")

    override fun init(context: ProcessorContext<Long, ArenaArbeidssokerregisterTilstand>?) {
        super.init(context)
        this.context = context
        keyValueStore = context?.getStateStore(stateStoreName)
    }

    override fun process(record: Record<Long, Pair<OpplysningerOmArbeidssoeker, Profilering>>?) {
        if (record == null) return
        process(
            requireNotNull(keyValueStore) { "TilstandsDb er ikke initialisert" },
            requireNotNull(context) { "Context er ikke satt" },
            record
        )
    }

    private fun process(
        db: KeyValueStore<Long, PeriodeListe>,
        context: ProcessorContext<Long, ArenaArbeidssokerregisterTilstand>,
        record: Record<Long, Pair<OpplysningerOmArbeidssoeker, Profilering>>
    ) {
        val periode = db.get(record.key())
            ?.perioder
            ?.firstOrNull { periode -> periode.id == record.value().first.periodeId }
        if (periode != null) {
            val arenaTilstand = byggArenaTilstand(periode, record.value().first, record.value().second)
                if (arenaTilstand.isValid) {
                    context.forward(record.withValue(arenaTilstand))
                    db.delete(record.key())
                } else {
                    joinErrorLogger.warn("Ignorer ugyldig match: {}", arenaTilstand.info())
                }
        } else {
            logger.debug(
                "Ignorerte key={}, opplysningsId={}, profileringsId={}, grunnet manglende periode(id={})",
                record.key(),
                record.value().first.id,
                record.value().second.id,
                record.value().first.periodeId
            )
        }
    }


    override fun close() {
        super.close()
        context = null
        keyValueStore = null
    }
}
