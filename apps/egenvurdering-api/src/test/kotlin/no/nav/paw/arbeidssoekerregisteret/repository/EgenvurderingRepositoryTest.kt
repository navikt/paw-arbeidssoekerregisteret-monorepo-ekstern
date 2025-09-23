package no.nav.paw.arbeidssoekerregisteret.repository

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.prosesserPerioderOgProfileringer
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingPostgresRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingPostgresRepository.lagreEgenvurdering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_GODE_MULIGHETER
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import no.nav.paw.test.data.periode.createEgenvurderingFor
import no.nav.paw.test.data.periode.createProfilering
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database.Companion.connect
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.util.*

class EgenvurderingRepositoryTest : FreeSpec({

    val dataSource = autoClose(initTestDatabase())
    beforeSpec { connect(dataSource) }


    "Sletter periode, profileringer og egenvurdering når stop-hendelse mottas for eksisterende periode" {
        val periodeId = UUID.randomUUID()
        val ident = "70000000000"

        val startHendelse = PeriodeFactory.create().build(
            id = periodeId,
            identitetsnummer = ident,
            avsluttet = null
        )
        val periodeStoppet = Instant.parse("2025-01-01T12:00:00Z")
        val stoppHendelse = PeriodeFactory.create().build(
            id = periodeId,
            identitetsnummer = ident,
            avsluttet = MetadataFactory.create().build(tidspunkt = periodeStoppet)
        )
        val profilering = createProfilering(periodeId = periodeId)
        val profilering2 = createProfilering(periodeId = periodeId)
        val egenvurdering = createEgenvurderingFor(profilering)
        val egenvurdering2 = createEgenvurderingFor(profilering2)

        recordSequence(startHendelse, profilering, profilering2).prosesserPerioderOgProfileringer()
        lagreEgenvurdering(egenvurdering)
        lagreEgenvurdering(egenvurdering2)
        recordSequence(stoppHendelse).prosesserPerioderOgProfileringer()

        transaction {
            PeriodeTable
                .selectAll()
                .where { PeriodeTable.id eq periodeId }
                .toList().size shouldBe 0

            ProfileringTable
                .selectAll()
                .where { ProfileringTable.periodeId eq periodeId }
                .toList().size shouldBe 0

            EgenvurderingTable.selectAll().toList().size shouldBe 0
        }
    }

    "Idempotens" - {
        "Duplikate profileringer" - {
            val periodeId = UUID.randomUUID()
            val ident = "44444444444"
            val periode = PeriodeFactory.create().build(id = periodeId, identitetsnummer = ident)
            val profilering = createProfilering(id = UUID.randomUUID(), periodeId = periodeId)
            val duplikatProfilering = createProfilering(id = profilering.id, periodeId = profilering.periodeId)

            shouldNotThrowAny {
                recordSequence(periode, profilering, duplikatProfilering).prosesserPerioderOgProfileringer()
            }

            transaction {
                ProfileringTable
                    .selectAll()
                    .where { ProfileringTable.id eq profilering.id }
                    .toList().size shouldBe 1
            }
        }

        "Duplikate start periode hendelser" - {
            val periodeId = UUID.randomUUID()
            val ident = "80000000000"

            val startHendelse = PeriodeFactory.create().build(
                id = periodeId,
                identitetsnummer = ident,
            )
            val duplikatStartHendelse = PeriodeFactory.create().build(
                id = periodeId,
                identitetsnummer = ident,
            )

            shouldNotThrowAny {
                recordSequence(startHendelse, duplikatStartHendelse).prosesserPerioderOgProfileringer()
            }

            transaction {
                val rader = PeriodeTable
                    .selectAll()
                    .where { PeriodeTable.id eq periodeId }
                    .toList()
                rader.size shouldBe 1
                rader.single()[PeriodeTable.avsluttet] shouldBe null
            }
        }

        "Duplikate stop periode hendelser" - {
            val startHendelse = PeriodeFactory.create().build(
                id = UUID.randomUUID(),
                avsluttet = null,
            )
            val stopHendelse = PeriodeFactory.create().build(
                id = startHendelse.id,
                avsluttet = MetadataFactory.create().build(tidspunkt = Instant.now()),
            )
            val duplikatStopHendelse = PeriodeFactory.create().build(
                id = stopHendelse.id,
                avsluttet = stopHendelse.avsluttet,
            )

            shouldNotThrowAny {
                recordSequence(startHendelse, stopHendelse, duplikatStopHendelse).prosesserPerioderOgProfileringer()
            }

            transaction {
                PeriodeTable
                    .selectAll()
                    .where { PeriodeTable.id eq startHendelse.id }
                    .toList().size shouldBe 0
            }
        }
    }


    "Returnerer nyeste profilering fra åpen periode uten egenvurdering" {
        val periodeId = UUID.randomUUID()
        val ident = "12345678910"
        val periode = PeriodeFactory.create().build(id = periodeId, identitetsnummer = ident)

        val eldreProfilering = createProfilering(
            periodeId = periodeId,
            sendtInnAv = MetadataFactory.create().build(
                tidspunkt = Instant.parse("2024-01-01T00:00:00Z")
            )
        )
        val nyereProfilering = createProfilering(
            periodeId = periodeId,
            sendtInnAv = MetadataFactory.create().build(
                tidspunkt = Instant.parse("2025-01-01T00:00:00Z")
            )
        )

        recordSequence(periode, eldreProfilering, nyereProfilering)
            .prosesserPerioderOgProfileringer(EgenvurderingPostgresRepository)

        val nyesteProfilering = finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)

        nyesteProfilering.shouldNotBeNull()
        nyesteProfilering.id shouldBe nyereProfilering.id
        nyesteProfilering.profilertTil shouldBe nyereProfilering.profilertTil.name
    }

    "Returnerer null når egenvurdering finnes for nyeste profilering" {
        val periodeId = UUID.randomUUID()
        val ident = "10987654321"
        val periode = PeriodeFactory.create().build(id = periodeId, identitetsnummer = ident)
        val profilering = createProfilering(periodeId = periodeId)
        val egenvurdering = createEgenvurderingFor(profilering)

        recordSequence(periode, profilering).prosesserPerioderOgProfileringer(EgenvurderingPostgresRepository)

        lagreEgenvurdering(egenvurdering)

        val nyesteProfilering = finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        nyesteProfilering.shouldBeNull()
    }

    "Returnerer null når perioden er avsluttet" {
        val periodeId = UUID.randomUUID()
        val ident = "55555555555"
        val avsluttetPeriode = PeriodeFactory.create().build(
            id = periodeId,
            identitetsnummer = ident,
            avsluttet = MetadataFactory.create().build()
        )
        val profilering = createProfilering(periodeId = periodeId)

        recordSequence(avsluttetPeriode, profilering).prosesserPerioderOgProfileringer(EgenvurderingPostgresRepository)

        val nyesteProfilering = finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        nyesteProfilering.shouldBeNull()
    }

    "Gjør ingenting for ukjent Avro type" {
        val ustøttetRecord = OpplysningerOmArbeidssoeker()
        val records = recordSequence(ustøttetRecord)

        shouldNotThrowAny {
            records.prosesserPerioderOgProfileringer(EgenvurderingPostgresRepository)
        }
    }

    "Gjør ingenting med tom records batch" {
        val ingenRecords = ConsumerRecords<Long, SpecificRecord>(emptyMap(), emptyMap())

        shouldNotThrowAny {
            ingenRecords.asSequence().prosesserPerioderOgProfileringer(EgenvurderingPostgresRepository)
        }
    }

    "Lagrer egenvurdering (brukerens egenvurdering, ikke profilertTil)" {
        val periodeId = UUID.randomUUID()
        val ident = "22222222222"
        val periode = PeriodeFactory.create().build(id = periodeId, identitetsnummer = ident)
        val profilering = createProfilering(
            periodeId = periodeId,
            profilertTil = ANTATT_BEHOV_FOR_VEILEDNING
        )
        val egenvurdering = createEgenvurderingFor(
            profilering,
            egenvurdering = ANTATT_GODE_MULIGHETER,
            profilertTil = profilering.profilertTil,
        )

        recordSequence(periode, profilering).prosesserPerioderOgProfileringer(EgenvurderingPostgresRepository)

        lagreEgenvurdering(egenvurdering)

        transaction {
            val lagretEgenvurdering = EgenvurderingTable
                .selectAll()
                .where { EgenvurderingTable.profileringId eq profilering.id }
                .single()[EgenvurderingTable.egenvurdering]
            lagretEgenvurdering shouldBe ANTATT_GODE_MULIGHETER.name
        }
    }
})

fun recordSequence(
    vararg values: SpecificRecord,
    topic: String = "test-topic",
    partition: Int = 0,
    startOffset: Long = 0L,
    key: Long = 123L,
): Sequence<ConsumerRecord<Long, SpecificRecord>> {
    val tp = TopicPartition(topic, partition)

    val list: List<ConsumerRecord<Long, SpecificRecord>> =
        values.mapIndexed { index, specificRecord ->
            ConsumerRecord(topic, partition, startOffset + index, key, specificRecord)
        }

    val nextOffset = if (list.isEmpty()) startOffset else list.last().offset() + 1
    val nextOffsets = mapOf(tp to OffsetAndMetadata(nextOffset))

    return ConsumerRecords(mapOf(tp to list), nextOffsets).asSequence()
}
