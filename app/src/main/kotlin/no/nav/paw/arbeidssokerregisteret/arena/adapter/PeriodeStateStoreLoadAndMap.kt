package no.nav.paw.arbeidssokerregisteret.arena.adapter

import no.nav.paw.arbeidssokerregisteret.api.v1.Beskrivelse
import no.nav.paw.arbeidssokerregisteret.api.v1.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v3.Utdanning
import no.nav.paw.arbeidssokerregisteret.arena.v1.Annet
import no.nav.paw.arbeidssokerregisteret.arena.v1.Arbeidserfaring
import no.nav.paw.arbeidssokerregisteret.arena.v1.Helse
import no.nav.paw.arbeidssokerregisteret.arena.v3.ArenaArbeidssokerregisterTilstand
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
    private var keyValueStore: KeyValueStore<Long, Periode>? = null
    private var context: ProcessorContext<Long, ArenaArbeidssokerregisterTilstand>? = null
    private val logger = LoggerFactory.getLogger(this::class.java)

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
        db: KeyValueStore<Long, Periode>,
        context: ProcessorContext<Long, ArenaArbeidssokerregisterTilstand>,
        record: Record<Long, Pair<OpplysningerOmArbeidssoeker, Profilering>>
    ) {
        val periode = db.get(record.key())
        if (periode != null) {
            val arenaTilstand = byggArenaTilstand(periode, record.value().first, record.value().second)
            context.forward(record.withValue(arenaTilstand))
            db.delete(record.key())
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

fun byggArenaTilstand(
    periode: Periode,
    opplysninger: OpplysningerOmArbeidssoeker,
    profilering: Profilering
): ArenaArbeidssokerregisterTilstand {
    val periode = no.nav.paw.arbeidssokerregisteret.arena.v1.Periode(
        periode.id,
        periode.identitetsnummer,
        periode.startet.toArena(),
        periode.avsluttet.toArena()
    )
    val opplysninger = no.nav.paw.arbeidssokerregisteret.arena.v3.OpplysningerOmArbeidssoeker(
        opplysninger.id,
        opplysninger.periodeId,
        opplysninger.sendtInnAv.toArena(),
        opplysninger.utdanning.toArena(),
        Helse(opplysninger.helse.helsetilstandHindrerArbeid.toArena()),
        Arbeidserfaring(opplysninger.arbeidserfaring.harHattArbeid.toArena()),
        opplysninger.jobbsituasjon.toArena(),
        Annet(opplysninger.annet.andreForholdHindrerArbeid.toArena())
    )
    val profilering = no.nav.paw.arbeidssokerregisteret.arena.v1.Profilering(
        profilering.id,
        profilering.periodeId,
        profilering.opplysningerOmArbeidssokerId,
        profilering.sendtInnAv.toArena(),
        profilering.profilertTil.toArena(),
        profilering.jobbetSammenhengendeSeksAvTolvSisteMnd,
        profilering.alder
    )
    return ArenaArbeidssokerregisterTilstand(
        periode,
        profilering,
        opplysninger
    )
}

fun ProfilertTil.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil =
    when (this) {
        ProfilertTil.UKJENT_VERDI -> no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil.UKJENT_VERDI
        ProfilertTil.UDEFINERT -> no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil.UDEFINERT
        ProfilertTil.ANTATT_GODE_MULIGHETER -> no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil.ANTATT_GODE_MULIGHETER
        ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        ProfilertTil.OPPGITT_HINDRINGER -> no.nav.paw.arbeidssokerregisteret.arena.v1.ProfilertTil.OPPGITT_HINDRINGER
    }

fun Jobbsituasjon.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.Jobbsituasjon =
    no.nav.paw.arbeidssokerregisteret.arena.v1.Jobbsituasjon(
        this.beskrivelser.map { it.toArena() }
    )

fun BeskrivelseMedDetaljer.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.BeskrivelseMedDetaljer =
    no.nav.paw.arbeidssokerregisteret.arena.v1.BeskrivelseMedDetaljer(
        beskrivelse.toArena(),
        detaljer
    )

fun Beskrivelse.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse =
    when (this) {
        Beskrivelse.UKJENT_VERDI -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.UKJENT_VERDI
        Beskrivelse.UDEFINERT -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.UDEFINERT
        Beskrivelse.HAR_SAGT_OPP -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.HAR_SAGT_OPP
        Beskrivelse.HAR_BLITT_SAGT_OPP -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.HAR_BLITT_SAGT_OPP
        Beskrivelse.ER_PERMITTERT -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.ER_PERMITTERT
        Beskrivelse.ALDRI_HATT_JOBB -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.ALDRI_HATT_JOBB
        Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR
        Beskrivelse.AKKURAT_FULLFORT_UTDANNING -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.AKKURAT_FULLFORT_UTDANNING
        Beskrivelse.VIL_BYTTE_JOBB -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.VIL_BYTTE_JOBB
        Beskrivelse.USIKKER_JOBBSITUASJON -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.USIKKER_JOBBSITUASJON
        Beskrivelse.MIDLERTIDIG_JOBB -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.MIDLERTIDIG_JOBB
        Beskrivelse.DELTIDSJOBB_VIL_MER -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.DELTIDSJOBB_VIL_MER
        Beskrivelse.NY_JOBB -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.NY_JOBB
        Beskrivelse.KONKURS -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.KONKURS
        Beskrivelse.ANNET -> no.nav.paw.arbeidssokerregisteret.arena.v1.Beskrivelse.ANNET
    }

fun Utdanning.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v3.Utdanning =
    no.nav.paw.arbeidssokerregisteret.arena.v3.Utdanning(
        nus,
        bestaatt.toArena(),
        godkjent.toArena(),
    )

fun JaNeiVetIkke.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.JaNeiVetIkke =
    when (this) {
        JaNeiVetIkke.JA -> no.nav.paw.arbeidssokerregisteret.arena.v1.JaNeiVetIkke.JA
        JaNeiVetIkke.NEI -> no.nav.paw.arbeidssokerregisteret.arena.v1.JaNeiVetIkke.NEI
        JaNeiVetIkke.VET_IKKE -> no.nav.paw.arbeidssokerregisteret.arena.v1.JaNeiVetIkke.VET_IKKE
    }

fun Metadata.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.Metadata =
    no.nav.paw.arbeidssokerregisteret.arena.v1.Metadata(
        tidspunkt,
        utfoertAv.toArena(),
        kilde,
        aarsak
    )

fun Bruker.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.Bruker =
    no.nav.paw.arbeidssokerregisteret.arena.v1.Bruker(
        type.toArena(),
        id
    )

fun BrukerType.toArena(): no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType =
    when (this) {
        BrukerType.SYSTEM -> no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType.SYSTEM
        BrukerType.VEILEDER -> no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType.VEILEDER
        BrukerType.SLUTTBRUKER -> no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType.SLUTTBRUKER
        BrukerType.UKJENT_VERDI -> no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType.UKJENT_VERDI
        BrukerType.UDEFINERT -> no.nav.paw.arbeidssokerregisteret.arena.v1.BrukerType.UDEFINERT
    }