package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.initTestDatabase
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.nyOpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.nyProfilering
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.shouldBeEqualTo
import org.jetbrains.exposed.sql.Database
import java.util.*
import javax.sql.DataSource

class ProfileringRepositoryTest : StringSpec({
    lateinit var dataSource: DataSource
    lateinit var database: Database
    lateinit var repository: ProfileringRepository
    val periodeId1: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1feb")
    val periodeId2: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1fec")
    val opplysningerId1: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1fed")
    val opplysningerId2: UUID = UUID.fromString("84201f96-363b-4aab-a589-89fa4b9b1fee")

    beforeEach {
        dataSource = initTestDatabase()
        database = Database.connect(dataSource)
        repository = ProfileringRepository(database)
        val opplysningerRepository = OpplysningerRepository(database)
        val opplysninger1 = nyOpplysningerOmArbeidssoeker(periodeId = periodeId1, opplysningerId = opplysningerId1)
        val opplysninger2 = nyOpplysningerOmArbeidssoeker(periodeId = periodeId2, opplysningerId = opplysningerId2)
        opplysningerRepository.lagreOpplysninger(opplysninger1)
        opplysningerRepository.lagreOpplysninger(opplysninger2)
    }

    afterEach {
        dataSource.connection.close()
    }

    "Opprett og hent ut en profilering" {
        val profilering = nyProfilering(periodeId1, opplysningerId1)
        repository.lagreProfilering(profilering)

        val profileringResponser = repository.finnProfileringerForPeriodeId(profilering.periodeId)

        profileringResponser.size shouldBe 1
        val profileringResponse = profileringResponser[0]
        profileringResponse shouldBeEqualTo profilering
    }

    "Opprett og hent ut flere profileringer" {
        val profilering1 = nyProfilering(periodeId1, opplysningerId1)
        val profilering2 = nyProfilering(periodeId1, opplysningerId2)
        repository.lagreProfilering(profilering1)
        repository.lagreProfilering(profilering2)

        val profileringResponser = repository.finnProfileringerForPeriodeId(periodeId1)

        profileringResponser.size shouldBe 2
        val profileringResponse1 = profileringResponser[0]
        profileringResponse1 shouldBeEqualTo profilering1
        val profileringResponse2 = profileringResponser[1]
        profileringResponse2 shouldBeEqualTo profilering2
    }

    "Hent ut profilering med PeriodeId" {
        val profilering = nyProfilering(periodeId1, opplysningerId1)
        repository.lagreProfilering(profilering)
        val profileringResponser = repository.finnProfileringerForPeriodeId(periodeId1)

        profileringResponser.size shouldBe 1
        val profileringResponse = profileringResponser[0]
        profileringResponse shouldBeEqualTo profilering
    }

    "Hent ut ikke-eksisterende profilering" {
        val profileringResponser = repository.finnProfileringerForPeriodeId(UUID.randomUUID())

        profileringResponser.size shouldBe 0
    }

    "Lagre profileringer med samme periodeId i batch" {
        val periodeId = UUID.randomUUID()
        val profilering1 = nyProfilering(periodeId, UUID.randomUUID())
        val profilering2 = nyProfilering(periodeId, UUID.randomUUID())
        val profilering3 = nyProfilering(periodeId, UUID.randomUUID())
        val profileringer = sequenceOf(profilering1, profilering2, profilering3)
        repository.lagreAlleProfileringer(profileringer)

        val lagredeProfileringer = repository.finnProfileringerForPeriodeId(periodeId)

        lagredeProfileringer.size shouldBeExactly 3
        val lagredeProfilering1 = lagredeProfileringer[0]
        val lagredeProfilering2 = lagredeProfileringer[1]
        val lagredeProfilering3 = lagredeProfileringer[2]
        lagredeProfilering1 shouldBeEqualTo profilering1
        lagredeProfilering2 shouldBeEqualTo profilering2
        lagredeProfilering3 shouldBeEqualTo profilering3
    }
})
