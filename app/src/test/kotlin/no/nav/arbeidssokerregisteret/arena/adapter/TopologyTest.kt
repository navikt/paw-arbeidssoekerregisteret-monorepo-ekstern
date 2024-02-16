package no.nav.arbeidssokerregisteret.arena.adapter

import io.confluent.kafka.schemaregistry.testutil.MockSchemaRegistry
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.arbeidssokerregisteret.arena.adapter.utils.opplysninger
import no.nav.arbeidssokerregisteret.arena.adapter.utils.periode
import no.nav.arbeidssokerregisteret.arena.adapter.utils.profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v3.OpplysningerOmArbeidssoeker
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.Serde
import java.util.concurrent.atomic.AtomicLong
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as ApiMetadata
import no.nav.paw.arbeidssokerregisteret.arena.v1.Metadata as ArenaMetadata
import no.nav.paw.arbeidssokerregisteret.arena.v1.Periode as ArenaPeriode
import no.nav.paw.arbeidssokerregisteret.arena.v1.Profilering as ArenaProfilering
import no.nav.paw.arbeidssokerregisteret.arena.v3.OpplysningerOmArbeidssoeker as ArenaOpplysninger

class TopologyTest : FreeSpec({
    with(testScope()) {
        val keySequence = AtomicLong(0)
        "Når vi har sendt, periode, opplysninger og profilering skal vi få noe ut på arena topic" - {
            val periode = keySequence.incrementAndGet() to periode(identietsnummer = "12345678901")
            "Når bare perioden er sendt inn skal vi ikke få noe ut på arena topic" {
                periodeTopic.pipeInput(periode.key, periode.melding)
                arenaTopic.isEmpty shouldBe true
            }
            val opplysninger = periode.key to opplysninger(periode = periode.melding.id)
            "Når opplysningene blir tilgjengelig sammen med perioden skal vi få noe ut på arena topic" {
                opplysningerTopic.pipeInput(opplysninger.key, opplysninger.melding )
                arenaTopic.isEmpty shouldBe true
            }
            val profilering = opplysninger.key to profilering(
                opplysningerId = opplysninger.melding.id,
                periode = opplysninger.melding.periodeId
            )
            "Når profileringen ankommer og periode og opplysninger er tilgjengelig skal vi få noe ut på arena topic" {
                profileringsTopic.pipeInput(profilering.key, profilering.melding)
                arenaTopic.isEmpty shouldBe false
                val (key, arenaTilstand) = arenaTopic.readKeyValue().let { it.key to it.value }
                key shouldBe periode.key
                assertApiPeriodeMatchesArenaPeriode(periode.melding, arenaTilstand.periode)
                assertApiOpplysningerMatchesArenaOpplysninger(opplysninger.melding, arenaTilstand.opplysningerOmArbeidssoeker)
                assertApiProfileringMatchesArenaProfilering(profilering.melding, arenaTilstand.profilering)
            }
        }
    }
})

fun assertApiOpplysningerMatchesArenaOpplysninger(api: OpplysningerOmArbeidssoeker,arena: ArenaOpplysninger) {
    arena.id shouldBe api.id
    arena.periodeId shouldBe api.periodeId
    arena.utdanning.nus shouldBe api.utdanning.nus
    arena.helse.helsetilstandHindrerArbeid.name shouldBe api.helse.helsetilstandHindrerArbeid.name
    arena.arbeidserfaring.harHattArbeid.name shouldBe api.arbeidserfaring.harHattArbeid.name
    assertApiMetadataMatchesArenaMetadata(api.sendtInnAv, arena.sendtInnAv)
}

fun assertApiPeriodeMatchesArenaPeriode(api: Periode, arena: ArenaPeriode) {
    arena.id shouldBe api.id
    arena.identitetsnummer shouldBe api.identitetsnummer
    assertApiMetadataMatchesArenaMetadata(api.startet, arena.startet)
    assertApiMetadataMatchesArenaMetadata(api.avsluttet, arena.avsluttet)
}

fun assertApiProfileringMatchesArenaProfilering(api: Profilering, arena: ArenaProfilering) {
    arena.id shouldBe api.id
    arena.opplysningerOmArbeidssokerId shouldBe api.opplysningerOmArbeidssokerId
    arena.periodeId shouldBe api.periodeId
    arena.profilertTil.name shouldBe api.profilertTil.name
    arena.alder shouldBe api.alder
    assertApiMetadataMatchesArenaMetadata(api.sendtInnAv, arena.sendtInnAv)
    arena.jobbetSammenhengendeSeksAvTolvSisteMnd shouldBe api.jobbetSammenhengendeSeksAvTolvSisteMnd
}

fun assertApiMetadataMatchesArenaMetadata(api: ApiMetadata?, arena: ArenaMetadata?) {
    if (api == null) {
        arena shouldBe null
        return
    } else {
        arena.shouldNotBeNull()
        arena.tidspunkt shouldBe api.tidspunkt
        arena.aarsak shouldBe api.aarsak
        arena.kilde shouldBe api.kilde
        arena.utfoertAv.id shouldBe api.utfoertAv.id
        arena.utfoertAv.type.name shouldBe api.utfoertAv.type.name
    }
}

inline fun <reified T : SpecificRecord> createAvroSerde(): Serde<T> {
    val SCHEMA_REGISTRY_SCOPE = "mock"
    return SpecificAvroSerde<T>(MockSchemaRegistry.getClientForScope(SCHEMA_REGISTRY_SCOPE)).apply {
        configure(
            mapOf(
                KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS to "true",
                KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG to "mock://$SCHEMA_REGISTRY_SCOPE"
            ),
            false
        )
    }
}

val Pair<Long, *>.key get() : Long = first
val <A: SpecificRecord> Pair<Long, A>.melding get(): A = second

