package no.nav.paw.arbeidssoekerregisteret.repository

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingPostgresRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingPostgresRepository.lagreEgenvurdering
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingPostgresRepository.lagrePerioderOgProfileringer
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
import org.jetbrains.exposed.sql.Database.Companion.connect
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.postgresql.util.PSQLException
import java.time.Instant
import java.util.*

class EgenvurderingRepositoryTest : FreeSpec({

    val dataSource = autoClose(initTestDatabase())
    beforeSpec { connect(dataSource) }

    "Ignorerer duplikat profilering" {
        val periodeId = UUID.randomUUID()
        val ident = "44444444444"
        val periode = PeriodeFactory.create().build(id = periodeId, identitetsnummer = ident)
        val profilering = createProfilering(id = UUID.randomUUID(), periodeId = periodeId)
        val duplikatProfilering = createProfilering(id = profilering.id, periodeId = profilering.periodeId)

        shouldNotThrow<PSQLException> {
            lagrePerioderOgProfileringer(recordsOf(periode, profilering, duplikatProfilering))
        }

        transaction {
            val rader = ProfileringTable
                .selectAll()
                .where { ProfileringTable.id eq profilering.id }
                .toList()

            rader.size shouldBe 1
        }
    }

    "Oppdaterer avsluttet tidspunkt når stop-hendelse mottas for eksisterende periode" {
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

        lagrePerioderOgProfileringer(recordsOf(startHendelse, stoppHendelse))

        transaction {
            val rader = PeriodeTable
                .selectAll()
                .where { PeriodeTable.id eq periodeId }
                .toList()
            rader.size shouldBe 1
            rader.single()[PeriodeTable.avsluttet] shouldBe periodeStoppet
        }
    }

    "Ignorer duplikat start periode hendelse" {
        val periodeId = UUID.randomUUID()
        val ident = "80000000000"

        val startHendelse = PeriodeFactory.create().build(
            id = periodeId,
            identitetsnummer = ident,
            avsluttet = null
        )
        val duplikatStartHendelse = PeriodeFactory.create().build(
            id = periodeId,
            identitetsnummer = ident,
            avsluttet = null
        )

        lagrePerioderOgProfileringer(
            recordsOf(startHendelse, duplikatStartHendelse)
        )

        transaction {
            val rader = PeriodeTable
                .selectAll()
                .where { PeriodeTable.id eq periodeId }
                .toList()
            rader.size shouldBe 1
            rader.single()[PeriodeTable.avsluttet] shouldBe null
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

        lagrePerioderOgProfileringer(
            recordsOf(periode, eldreProfilering, nyereProfilering)
        )

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

        lagrePerioderOgProfileringer(recordsOf(periode, profilering))
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

        lagrePerioderOgProfileringer(recordsOf(avsluttetPeriode, profilering))

        val nyesteProfilering = finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        nyesteProfilering.shouldBeNull()
    }

    "Gjør ingenting for ukjent Avro type" {
        val ustøttetRecord = OpplysningerOmArbeidssoeker()
        val records = recordsOf(ustøttetRecord)

        shouldNotThrowAny {
            lagrePerioderOgProfileringer(records)
        }
    }

    "Gjør ingenting med tom records batch" {
        val ingenRecords = ConsumerRecords<Long, SpecificRecord>(emptyMap(), emptyMap())

        shouldNotThrowAny {
            lagrePerioderOgProfileringer(ingenRecords)
        }
    }

    "Lagrer egenvurdering (lagrer brukerens egenvurdering, ikke profilertTil)" {
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

        lagrePerioderOgProfileringer(recordsOf(periode, profilering))
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

private fun recordsOf(
    vararg values: SpecificRecord,
    topic: String = "test-topic",
    partition: Int = 0,
    startOffset: Long = 0L,
    key: Long = 123L,
): ConsumerRecords<Long, SpecificRecord> {
    val tp = TopicPartition(topic, partition)

    val list: List<ConsumerRecord<Long, SpecificRecord>> =
        values.mapIndexed { index, specificRecord ->
            ConsumerRecord(topic, partition, startOffset + index, key, specificRecord)
        }

    val nextOffset = if (list.isEmpty()) startOffset else list.last().offset() + 1
    val nextOffsets = mapOf(tp to OffsetAndMetadata(nextOffset))

    return ConsumerRecords(mapOf(tp to list), nextOffsets)
}
