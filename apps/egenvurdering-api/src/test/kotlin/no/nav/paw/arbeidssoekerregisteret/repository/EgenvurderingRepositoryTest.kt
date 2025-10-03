package no.nav.paw.arbeidssoekerregisteret.repository

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.context.consumerVersion
import no.nav.paw.arbeidssoekerregisteret.context.partitionCount
import no.nav.paw.arbeidssoekerregisteret.hwm.initHwm
import no.nav.paw.arbeidssoekerregisteret.lagrePerioderOgProfileringer
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingPostgresRepository.finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingPostgresRepository.finnProfilering
import no.nav.paw.arbeidssoekerregisteret.repository.EgenvurderingPostgresRepository.lagreEgenvurdering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil.ANTATT_GODE_MULIGHETER
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.model.Identitetsnummer
import no.nav.paw.test.data.periode.MetadataFactory
import no.nav.paw.test.data.periode.PeriodeFactory
import no.nav.paw.test.data.periode.createEgenvurderingFor
import no.nav.paw.test.data.periode.createProfilering
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class EgenvurderingRepositoryTest : FreeSpec({

    val dataSource = autoClose(initTestDatabase())
    beforeSpec {
        Database.connect(dataSource)
        transaction {
            initHwm(testTopic, consumerVersion, partitionCount)
        }
    }

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

        transaction {
            recordSequence(startHendelse, profilering, profilering2).lagrePerioderOgProfileringer()
            lagreEgenvurdering(egenvurdering)
            lagreEgenvurdering(egenvurdering2)
            recordSequence(stoppHendelse).lagrePerioderOgProfileringer()
        }
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
                transaction {
                    recordSequence(periode, profilering, duplikatProfilering).lagrePerioderOgProfileringer()
                }
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
                transaction {
                    recordSequence(startHendelse, duplikatStartHendelse).lagrePerioderOgProfileringer()
                }
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
                transaction {
                    recordSequence(startHendelse, stopHendelse, duplikatStopHendelse).lagrePerioderOgProfileringer()
                }
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
        val ident = Identitetsnummer("12345678910")
        val periode = PeriodeFactory.create().build(id = periodeId, identitetsnummer = ident.verdi)

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

        transaction {
            recordSequence(periode, eldreProfilering, nyereProfilering).lagrePerioderOgProfileringer()
        }

        val nyesteProfilering = finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)

        nyesteProfilering.shouldNotBeNull()
        nyesteProfilering.id shouldBe nyereProfilering.id
        nyesteProfilering.profilertTil shouldBe nyereProfilering.profilertTil.name
    }

    "Returnerer null når egenvurdering finnes for nyeste profilering" {
        val periodeId = UUID.randomUUID()
        val ident = Identitetsnummer("10987654321")
        val periode = PeriodeFactory.create().build(id = periodeId, identitetsnummer = ident.verdi)
        val profilering = createProfilering(periodeId = periodeId)
        val egenvurdering = createEgenvurderingFor(profilering)

        transaction {
            recordSequence(periode, profilering).lagrePerioderOgProfileringer()
            lagreEgenvurdering(egenvurdering)
        }

        val nyesteProfilering = finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        nyesteProfilering.shouldBeNull()
    }

    "Returnerer null når perioden er avsluttet" {
        val periodeId = UUID.randomUUID()
        val ident = Identitetsnummer("55555555555")
        val avsluttetPeriode = PeriodeFactory.create().build(
            id = periodeId,
            identitetsnummer = ident.verdi,
            avsluttet = MetadataFactory.create().build()
        )
        val profilering = createProfilering(periodeId = periodeId)

        transaction {
            recordSequence(avsluttetPeriode, profilering).lagrePerioderOgProfileringer()
        }
        val nyesteProfilering = finnNyesteProfileringFraÅpenPeriodeUtenEgenvurdering(ident)
        nyesteProfilering.shouldBeNull()
    }

    "Gjør ingenting for ukjent Avro type" {
        val ustøttetRecord = OpplysningerOmArbeidssoeker()

        shouldNotThrowAny {
            transaction {
                recordSequence(ustøttetRecord).lagrePerioderOgProfileringer()
            }
        }
    }

    "Gjør ingenting med tom records batch" {
        val ingenRecords = ConsumerRecords<Long, SpecificRecord>(emptyMap(), emptyMap())

        shouldNotThrowAny {
            transaction {
                ingenRecords.asSequence().lagrePerioderOgProfileringer()
            }
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

        transaction {
            recordSequence(periode, profilering).lagrePerioderOgProfileringer()
            lagreEgenvurdering(egenvurdering)
        }

        transaction {
            val lagretEgenvurdering = EgenvurderingTable
                .selectAll()
                .where { EgenvurderingTable.profileringId eq profilering.id }
                .single()[EgenvurderingTable.egenvurdering]
            lagretEgenvurdering shouldBe ANTATT_GODE_MULIGHETER.name
        }
    }

    "Finn profilering fra profileringId og ident" - {
        val startHendelse = PeriodeFactory.create().build()
        val profilering = createProfilering(periodeId = startHendelse.id)
        val profilering2 = createProfilering(periodeId = startHendelse.id)
        transaction {
            recordSequence(startHendelse, profilering, profilering2).lagrePerioderOgProfileringer()
        }
        val profileringRow = finnProfilering(profilering.id, Identitetsnummer(startHendelse.identitetsnummer))
        profileringRow.shouldNotBeNull()
        profileringRow.id shouldBe profilering.id
        profileringRow.periodeId shouldBe startHendelse.id
    }
})

private const val testTopic = "test-topic"

private val offsetCounter = AtomicLong(0L)

fun recordSequence(
    vararg values: SpecificRecord,
    topic: String = testTopic,
    partition: Int = 0,
    key: Long = 123L,
): Sequence<ConsumerRecord<Long, SpecificRecord>> {
    val tp = TopicPartition(topic, partition)

    val list: List<ConsumerRecord<Long, SpecificRecord>> =
        values.map { specificRecord ->
            ConsumerRecord(topic, partition, offsetCounter.getAndIncrement(), key, specificRecord)
        }

    return ConsumerRecords(mapOf(tp to list), emptyMap()).asSequence()
}
