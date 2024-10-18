package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.services.PeriodeService
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.copy
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.initTestDatabase
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.nyAvsluttetPeriode
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.nyBruker
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.nyMetadata
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.nyStartetPeriode
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.nyTidspunktFraKilde
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.shouldBeEqualTo
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import org.jetbrains.exposed.sql.Database
import java.time.Duration
import java.time.Instant
import javax.sql.DataSource

class PeriodeRepositoryTest : StringSpec({
    lateinit var dataSource: DataSource
    lateinit var database: Database
    lateinit var repository: PeriodeRepository

    beforeSpec {
        dataSource = initTestDatabase()
        database = Database.connect(dataSource)
        repository = PeriodeRepository(database)
    }

    afterSpec {
        dataSource.connection.close()
    }

    "Opprett og hent en periode" {
        val periode = nyStartetPeriode()
        repository.lagrePeriode(periode)

        val retrievedPeriode = repository.hentPeriodeForId(periode.id)

        retrievedPeriode shouldNotBe null
        retrievedPeriode!! shouldBe periode
    }

    "Hent en perioder for et gitt identitetsnummer" {
        val identitetsnummer = "31017098765"
        val periode1 = nyStartetPeriode(identitetsnummer = identitetsnummer)
        val periode2 = nyStartetPeriode(identitetsnummer = identitetsnummer)
        val periode3 = nyStartetPeriode(identitetsnummer = identitetsnummer)
        repository.lagrePeriode(periode1)
        repository.lagrePeriode(periode2)
        repository.lagrePeriode(periode3)
        val retrievedPerioder = repository.finnPerioderForIdentiteter(listOf(Identitetsnummer(identitetsnummer)))

        retrievedPerioder.size shouldBeExactly 3
        val retrievedPerioderMap = retrievedPerioder.associateBy { it.periodeId }
        val retrievedPeriode1 = retrievedPerioderMap[periode1.id]
        val retrievedPeriode2 = retrievedPerioderMap[periode2.id]
        val retrievedPeriode3 = retrievedPerioderMap[periode3.id]
        retrievedPeriode1 shouldNotBe null
        retrievedPeriode2 shouldNotBe null
        retrievedPeriode3 shouldNotBe null
        retrievedPeriode1!! shouldBeEqualTo periode1
        retrievedPeriode2!! shouldBeEqualTo periode2
        retrievedPeriode3!! shouldBeEqualTo periode3
    }

    "Oppdater åpen periode med avsluttet metadata" {
        val periode = nyStartetPeriode()
        repository.lagrePeriode(periode)
        val updatedPeriode = periode.copy(
            avsluttet = nyMetadata()
        )
        repository.lagrePeriode(updatedPeriode)

        val retrievedPeriode = repository.hentPeriodeForId(periode.id)

        retrievedPeriode shouldNotBe null
        retrievedPeriode!! shouldBe updatedPeriode
    }

    "Oppdater avsluttet periode med ny startet og avsluttet metadata" {
        val periode = nyAvsluttetPeriode()
        repository.lagrePeriode(periode)
        val updatedPeriode = periode.copy(
            startet = nyMetadata(
                tidspunkt = Instant.now(),
                bruker = nyBruker(id = "02027612345"),
                kilde = "NY_KILDE",
                aarsak = "NY_AARSAK",
                tidspunktFraKilde = nyTidspunktFraKilde(
                    tidspunkt = Instant.now(),
                    avviksType = AvviksType.RETTING
                )
            ),
            avsluttet = nyMetadata(
                bruker = nyBruker(type = BrukerType.SYSTEM, id = "ARENA")
            )
        )
        repository.lagrePeriode(updatedPeriode)

        val retrievedPeriode = repository.hentPeriodeForId(periode.id)

        retrievedPeriode shouldNotBe null
        retrievedPeriode shouldBe updatedPeriode
    }

    "Oppdatere avsluttet periode med null avsluttet metadata" {
        val periode = nyAvsluttetPeriode()
        repository.lagrePeriode(periode)
        val updatedPeriode = periode.copy(
            startet = nyMetadata(
                tidspunkt = Instant.now().minus(Duration.ofDays(2)),
                bruker = nyBruker(type = BrukerType.UDEFINERT, id = "98765"),
                kilde = "ANNEN_KILDE",
                aarsak = "ANNEN_AARSAK",
                tidspunktFraKilde = nyTidspunktFraKilde(
                    tidspunkt = Instant.now().minus(Duration.ofDays(1)),
                    avviksType = AvviksType.FORSINKELSE
                )
            ),
            avsluttet = null
        )
        repository.lagrePeriode(updatedPeriode)

        val retrievedPeriode = repository.hentPeriodeForId(periode.id)

        retrievedPeriode shouldNotBe null
        retrievedPeriode shouldBe updatedPeriode
    }

    "Lagre startede perioder i batch" {
        val periode1 = nyStartetPeriode(identitetsnummer = "01017012345")
        val periode2 = nyStartetPeriode(identitetsnummer = "02017012345")
        val perioder = sequenceOf(periode1, periode2)
        repository.lagreAllePerioder(perioder)

        val lagretPeriode1 = repository.hentPeriodeForId(periode1.id)
        val lagretPeriode2 = repository.hentPeriodeForId(periode2.id)

        lagretPeriode1 shouldNotBe null
        lagretPeriode2 shouldNotBe null
        lagretPeriode1!! shouldBe periode1
        lagretPeriode2!! shouldBe periode2
    }

    "Lagre noen avsluttede perioder i batch" {
        val periode1 = nyStartetPeriode(
            identitetsnummer = "01017012345",
            startetMetadata = nyMetadata(
                tidspunkt = Instant.now().minus(Duration.ofDays(1)),
                bruker = nyBruker(id = "01017012345")
            )
        )
        val periode2 = nyStartetPeriode(
            identitetsnummer = "02017012345",
            startetMetadata = nyMetadata(
                tidspunkt = Instant.now().minus(Duration.ofDays(2)),
                bruker = nyBruker(id = "02017012345")
            )
        )
        val periode3 = nyStartetPeriode(
            identitetsnummer = "03017012345",
            startetMetadata = nyMetadata(
                tidspunkt = Instant.now().minus(Duration.ofDays(3)),
                bruker = nyBruker(type = BrukerType.VEILEDER, id = "12345")
            )
        )
        val periode4 = periode1.copy(
            startet = nyMetadata(
                tidspunkt = Instant.now(),
                bruker = nyBruker(id = "02027612345"),
                kilde = "NY_KILDE",
                aarsak = "NY_AARSAK",
                tidspunktFraKilde = nyTidspunktFraKilde(
                    tidspunkt = Instant.now(),
                    avviksType = AvviksType.RETTING
                )
            ),
            avsluttet = nyMetadata(
                bruker = nyBruker(type = BrukerType.SYSTEM, id = "ARENA")
            )
        )
        val periode5 = periode2.copy(
            avsluttet = nyMetadata(
                bruker = nyBruker(type = BrukerType.SYSTEM, id = "ARENA")
            )
        )
        val startedePerioder = sequenceOf(periode1, periode2, periode3)
        val avsluttedePerioder = sequenceOf(periode4, periode5)
        repository.lagreAllePerioder(startedePerioder)
        repository.lagreAllePerioder(avsluttedePerioder)

        val lagretPeriode1 = repository.hentPeriodeForId(periode1.id)
        val lagretPeriode2 = repository.hentPeriodeForId(periode2.id)
        val lagretPeriode3 = repository.hentPeriodeForId(periode3.id)
        val lagretPeriode4 = repository.hentPeriodeForId(periode4.id)
        val lagretPeriode5 = repository.hentPeriodeForId(periode5.id)

        lagretPeriode1 shouldNotBe null
        lagretPeriode2 shouldNotBe null
        lagretPeriode3 shouldNotBe null
        lagretPeriode4 shouldNotBe null
        lagretPeriode5 shouldNotBe null

        lagretPeriode1!! shouldBe periode4
        lagretPeriode2!! shouldBe periode5
        lagretPeriode3!! shouldBe periode3
        lagretPeriode4!! shouldBe periode4
        lagretPeriode5!! shouldBe periode5
        lagretPeriode1 shouldBe lagretPeriode4
        lagretPeriode2 shouldBe lagretPeriode5
    }
})
