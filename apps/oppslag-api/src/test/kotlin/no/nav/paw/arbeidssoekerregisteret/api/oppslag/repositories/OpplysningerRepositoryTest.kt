package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.OpplysningerFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.PeriodeOpplysningerFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toOpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toPeriode
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.initTestDatabase
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.shouldBeEqualTo
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.*
import javax.sql.DataSource

class OpplysningerRepositoryTest : StringSpec({

    lateinit var dataSource: DataSource
    lateinit var database: Database
    lateinit var opplysningerRepository: OpplysningerRepository
    lateinit var periodeRepository: PeriodeRepository

    beforeSpec {
        dataSource = initTestDatabase()
        database = Database.connect(dataSource)
        opplysningerRepository = OpplysningerRepository()
        periodeRepository = PeriodeRepository()
        val periode1 = TestData.nyAvsluttetPeriodeRow(periodeId = TestData.periodeId1, identitetsnummer = TestData.fnr1)
            .toPeriode()
        val periode2 = TestData.nyAvsluttetPeriodeRow(periodeId = TestData.periodeId2, identitetsnummer = TestData.fnr2)
            .toPeriode()
        val periode3 = TestData.nyAvsluttetPeriodeRow(periodeId = TestData.periodeId3, identitetsnummer = TestData.fnr3)
            .toPeriode()
        val periode4 = TestData.nyAvsluttetPeriodeRow(periodeId = TestData.periodeId4, identitetsnummer = TestData.fnr4)
            .toPeriode()
        val periode5 = TestData.nyAvsluttetPeriodeRow(periodeId = TestData.periodeId5, identitetsnummer = TestData.fnr5)
            .toPeriode()
        val periode6 = TestData.nyAvsluttetPeriodeRow(periodeId = TestData.periodeId6, identitetsnummer = TestData.fnr6)
            .toPeriode()
        val periode7 = TestData.nyAvsluttetPeriodeRow(periodeId = TestData.periodeId7, identitetsnummer = TestData.fnr7)
            .toPeriode()
        val perioder = listOf(periode1, periode2, periode3, periode4, periode5, periode6, periode7)
        periodeRepository.lagreAllePerioder(perioder)
    }

    afterSpec {
        dataSource.connection.close()
    }

    "Opprett og hent ut opplysninger om arbeidssøker" {
        val opplysninger = TestData.nyOpplysningerRow(
            periodeId = TestData.periodeId1,
            opplysningerId = TestData.opplysningerId1
        )
        opplysningerRepository.lagreOpplysninger(opplysninger.toOpplysningerOmArbeidssoeker())

        val retrievedOpplysninger = opplysningerRepository
            .finnOpplysningerForPeriodeIdList(listOf(opplysninger.periodeId))
        val retrievedPeriodeOpplysninger = finnPeriodeOpplysninger(database, TestData.periodeId1)

        retrievedOpplysninger.size shouldBe 1
        val retrievedOpplysninger1 = retrievedOpplysninger[0]
        retrievedOpplysninger1 shouldBeEqualTo opplysninger
        retrievedPeriodeOpplysninger.size shouldBe 1
        val retrievedPeriodeOpplysninger1 = retrievedPeriodeOpplysninger[0]
        retrievedPeriodeOpplysninger1.periodeId shouldBe retrievedOpplysninger1.periodeId
    }

    "Opprett og hent ut opplysninger om arbeidssøker med utdanning, helse og annet lik null" {
        val opplysninger = TestData.nyOpplysningerRow(
            periodeId = TestData.periodeId2,
            opplysningerId = TestData.opplysningerId2,
            utdanning = null,
            helse = null,
            annet = null
        )
        opplysningerRepository.lagreOpplysninger(opplysninger.toOpplysningerOmArbeidssoeker())

        val retrievedOpplysninger = opplysningerRepository
            .finnOpplysningerForPeriodeIdList(listOf(opplysninger.periodeId))
        val retrievedPeriodeOpplysninger = finnPeriodeOpplysninger(database, TestData.periodeId2)

        retrievedOpplysninger.size shouldBe 1
        val retrievedOpplysninger1 = retrievedOpplysninger[0]
        retrievedOpplysninger1 shouldBeEqualTo opplysninger
        retrievedPeriodeOpplysninger.size shouldBe 1
        val retrievedPeriodeOpplysninger1 = retrievedPeriodeOpplysninger[0]
        retrievedPeriodeOpplysninger1.periodeId shouldBe retrievedOpplysninger1.periodeId
    }

    "Opprett og hent ut opplysninger om arbeidssøker med utdanning og annet med felter lik null" {
        val opplysninger = TestData.nyOpplysningerRow(
            periodeId = TestData.periodeId3,
            opplysningerId = TestData.opplysningerId3,
            utdanning = TestData.nyUtdanningRow(bestaatt = null, godkjent = null),
            annet = TestData.nyAnnetRow(andreForholdHindrerArbeid = null)
        )
        opplysningerRepository.lagreOpplysninger(opplysninger.toOpplysningerOmArbeidssoeker())

        val retrievedOpplysninger = opplysningerRepository
            .finnOpplysningerForPeriodeIdList(listOf(opplysninger.periodeId))
        val retrievedPeriodeOpplysninger = finnPeriodeOpplysninger(database, TestData.periodeId3)

        retrievedOpplysninger.size shouldBe 1
        val retrievedOpplysninger1 = retrievedOpplysninger[0]
        retrievedOpplysninger1 shouldBeEqualTo opplysninger
        retrievedPeriodeOpplysninger.size shouldBe 1
        val retrievedPeriodeOpplysninger1 = retrievedPeriodeOpplysninger[0]
        retrievedPeriodeOpplysninger1.periodeId shouldBe retrievedOpplysninger1.periodeId
    }

    "Opprett og hent ut flere opplysninger om arbeidssøker med samme periodeId" {
        val tidspunkt = Instant.now()
        val opplysninger1 = TestData.nyOpplysningerRow(
            periodeId = TestData.periodeId4,
            opplysningerId = TestData.opplysningerId4,
            sendtInnAv = TestData.nyMetadataRow(tidspunkt = tidspunkt)
        )
        val opplysninger2 = TestData.nyOpplysningerRow(
            periodeId = TestData.periodeId4,
            opplysningerId = TestData.opplysningerId5,
            sendtInnAv = TestData.nyMetadataRow(tidspunkt = tidspunkt.minusSeconds(60))
        )
        opplysningerRepository.lagreOpplysninger(opplysninger1.toOpplysningerOmArbeidssoeker())
        opplysningerRepository.lagreOpplysninger(opplysninger2.toOpplysningerOmArbeidssoeker())

        val retrievedOpplysninger = opplysningerRepository
            .finnOpplysningerForPeriodeIdList(listOf(TestData.periodeId4))
        val retrievedPeriodeOpplysninger = finnPeriodeOpplysninger(database, TestData.periodeId4)

        retrievedOpplysninger.size shouldBe 2
        val retrievedOpplysninger1 = retrievedOpplysninger[0]
        val retrievedOpplysninger2 = retrievedOpplysninger[1]
        retrievedOpplysninger1 shouldBeEqualTo opplysninger1
        retrievedOpplysninger2 shouldBeEqualTo opplysninger2
        retrievedPeriodeOpplysninger.size shouldBe 2
        val retrievedPeriodeOpplysninger1 = retrievedPeriodeOpplysninger[0]
        val retrievedPeriodeOpplysninger2 = retrievedPeriodeOpplysninger[1]
        retrievedPeriodeOpplysninger1.periodeId shouldBe retrievedOpplysninger1.periodeId
        retrievedPeriodeOpplysninger2.periodeId shouldBe retrievedOpplysninger2.periodeId
    }

    "Opprett og hent ut opplysninger om arbeidssøker med forskjellig periodeId" {
        val tidspunkt = Instant.now()
        val opplysninger1 = TestData
            .nyOpplysningerRow(
                periodeId = TestData.periodeId5,
                opplysningerId = TestData.opplysningerId6,
                sendtInnAv = TestData.nyMetadataRow(tidspunkt = tidspunkt)
            )
        val opplysninger2 =
            TestData.nyOpplysningerRow(
                periodeId = TestData.periodeId6,
                opplysningerId = TestData.opplysningerId7,
                sendtInnAv = TestData.nyMetadataRow(tidspunkt = tidspunkt.minusSeconds(60))
            )

        opplysningerRepository.lagreOpplysninger(opplysninger1.toOpplysningerOmArbeidssoeker())
        opplysningerRepository.lagreOpplysninger(opplysninger2.toOpplysningerOmArbeidssoeker())

        val retrievedOpplysninger =
            finnOpplysninger(database).filter { it.periodeId in listOf(TestData.periodeId5, TestData.periodeId6) }
        val retrievedPeriodeOpplysninger = finnPeriodeOpplysninger(database).filter {
            it.periodeId in listOf(
                TestData.periodeId5,
                TestData.periodeId6
            )
        }

        retrievedOpplysninger.size shouldBe 2
        val retrievedOpplysninger1 = retrievedOpplysninger[0]
        retrievedOpplysninger1.opplysningerId shouldBe opplysninger1.opplysningerId
        retrievedOpplysninger1.periodeId shouldBe opplysninger1.periodeId
        retrievedPeriodeOpplysninger.size shouldBe 2
        val retrievedPeriodeOpplysninger1 = retrievedPeriodeOpplysninger[0]
        retrievedPeriodeOpplysninger1.periodeId shouldBe opplysninger1.periodeId
        retrievedPeriodeOpplysninger1.opplysningerOmArbeidssoekerTableId shouldBe retrievedOpplysninger1.id
    }

    "Like opplysninger med samme periodeId skal ikke lagres på nytt" {
        val opplysninger1 = TestData
            .nyOpplysningerRow(
                periodeId = TestData.periodeId7,
                opplysningerId = TestData.opplysningerId8
            )
        val opplysninger2 = TestData
            .nyOpplysningerRow(
                periodeId = TestData.periodeId7,
                opplysningerId = TestData.opplysningerId8
            )

        opplysningerRepository.lagreOpplysninger(opplysninger1.toOpplysningerOmArbeidssoeker())
        opplysningerRepository.lagreOpplysninger(opplysninger2.toOpplysningerOmArbeidssoeker())

        val retrievedOpplysninger = opplysningerRepository
            .finnOpplysningerForPeriodeIdList(listOf(opplysninger1.periodeId))
        val retrievedPeriodeOpplysninger = finnPeriodeOpplysninger(database, TestData.periodeId7)

        retrievedOpplysninger.size shouldBe 1
        val retrievedOpplysninger1 = retrievedOpplysninger[0]
        retrievedOpplysninger1 shouldBeEqualTo opplysninger1
        retrievedPeriodeOpplysninger.size shouldBe 1
        val retrievedPeriodeOpplysninger1 = retrievedPeriodeOpplysninger[0]
        retrievedPeriodeOpplysninger1.periodeId shouldBe retrievedOpplysninger1.periodeId
    }

    "Hent ut ikke-eksisterende opplysninger om arbeidssøker" {
        val retrievedOpplysninger = opplysningerRepository
            .finnOpplysningerForPeriodeIdList(listOf(UUID.randomUUID()))

        retrievedOpplysninger.size shouldBe 0
    }

    "Lagre flere opplysninger og hent med identitetsnummer" {
        val periodeId = UUID.randomUUID()
        val tidspunkt = Instant.now()
        val periode = TestData.nyStartetPeriode(
            periodeId = periodeId,
            identitetsnummer = TestData.identitetsnummer8.verdi
        )
        periodeRepository.lagrePeriode(periode)
        val opplysninger1 = TestData.nyOpplysningerRow(
            periodeId = periodeId,
            opplysningerId = TestData.opplysningerId9,
            sendtInnAv = TestData.nyMetadataRow(tidspunkt = tidspunkt)
        )
        val opplysninger2 = TestData.nyOpplysningerRow(
            periodeId = periodeId,
            opplysningerId = TestData.opplysningerId10,
            sendtInnAv = TestData.nyMetadataRow(tidspunkt = tidspunkt.minusSeconds(60))
        )
        val opplysninger3 = TestData.nyOpplysningerRow(
            periodeId = periodeId,
            opplysningerId = TestData.opplysningerId11,
            sendtInnAv = TestData.nyMetadataRow(tidspunkt = tidspunkt.minusSeconds(120))
        )
        val opplysninger = listOf(
            opplysninger1.toOpplysningerOmArbeidssoeker(),
            opplysninger2.toOpplysningerOmArbeidssoeker(),
            opplysninger3.toOpplysningerOmArbeidssoeker()
        )
        opplysningerRepository.lagreAlleOpplysninger(opplysninger)

        val retrievedOpplysninger = opplysningerRepository
            .finnOpplysningerForIdentiteter(listOf(TestData.identitetsnummer8))

        retrievedOpplysninger.size shouldBe 3
        val retrievedOpplysninger1 = retrievedOpplysninger[0]
        val retrievedOpplysninger2 = retrievedOpplysninger[1]
        val retrievedOpplysninger3 = retrievedOpplysninger[2]
        retrievedOpplysninger1 shouldBeEqualTo opplysninger1
        retrievedOpplysninger2 shouldBeEqualTo opplysninger2
        retrievedOpplysninger3 shouldBeEqualTo opplysninger3
    }
})

private fun finnOpplysninger(database: Database) =
    transaction(database) {
        OpplysningerFunctions.finnRows()
    }


private fun finnPeriodeOpplysninger(database: Database, periodeId: UUID) =
    transaction(database) {
        PeriodeOpplysningerFunctions.findForPeriodeId(periodeId)
    }

private fun finnPeriodeOpplysninger(database: Database) =
    transaction(database) {
        PeriodeOpplysningerFunctions.findAll()
    }
