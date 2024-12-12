package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.toPeriode
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.TestData
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.initTestDatabase
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.test.shouldBeEqualTo
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType
import org.jetbrains.exposed.sql.Database
import java.time.Duration
import java.time.Instant
import javax.sql.DataSource

class PeriodeRepositoryTest : StringSpec({
    lateinit var dataSource: DataSource
    lateinit var repository: PeriodeRepository

    beforeSpec {
        dataSource = initTestDatabase()
        Database.connect(dataSource)
        repository = PeriodeRepository()
    }

    afterSpec {
        dataSource.connection.close()
    }

    "Opprett og hent en periode" {
        val periode = TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr1)
        repository.lagrePeriode(periode.toPeriode())

        val lagretPeriode = repository.hentPeriodeForId(periode.periodeId)

        periode shouldBeEqualTo lagretPeriode
    }

    "Hent en perioder for et gitt identitetsnummer" {
        val periode1 = TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr2)
        val periode2 = TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr2)
        val periode3 = TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr2)
        repository.lagrePeriode(periode1.toPeriode())
        repository.lagrePeriode(periode2.toPeriode())
        repository.lagrePeriode(periode3.toPeriode())
        val lagretPerioder = repository.finnPerioderForIdentiteter(listOf(TestData.identitetsnummer2))

        lagretPerioder.size shouldBeExactly 3
        val lagretPerioderMap = lagretPerioder.associateBy { it.periodeId }
        val lagretPeriode1 = lagretPerioderMap[periode1.periodeId]
        val lagretPeriode2 = lagretPerioderMap[periode2.periodeId]
        val lagretPeriode3 = lagretPerioderMap[periode3.periodeId]

        periode1 shouldBeEqualTo lagretPeriode1
        periode2 shouldBeEqualTo lagretPeriode2
        periode3 shouldBeEqualTo lagretPeriode3
    }

    "Oppdater åpen periode med avsluttet metadata" {
        val periode = TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr3)
        repository.lagrePeriode(periode.toPeriode())
        val updatedPeriode = periode.copy(
            avsluttet = TestData.nyMetadataRow()
        )
        repository.lagrePeriode(updatedPeriode.toPeriode())

        val lagretPeriode = repository.hentPeriodeForId(periode.periodeId)

        updatedPeriode shouldBeEqualTo lagretPeriode
    }

    "Oppdater avsluttet periode med ny startet og avsluttet metadata" {
        val periode = TestData.nyAvsluttetPeriodeRow(identitetsnummer = TestData.fnr4)
        repository.lagrePeriode(periode.toPeriode())
        val updatedPeriode = periode.copy(
            startet = TestData.nyMetadataRow(
                tidspunkt = Instant.now(),
                utfoertAv = TestData.nyBrukerRow(brukerId = TestData.fnr4),
                kilde = "NY_KILDE",
                aarsak = "NY_AARSAK",
                tidspunktFraKilde = TestData.nyTidspunktFraKildeRow(
                    tidspunkt = Instant.now(),
                    avviksType = AvviksType.RETTING
                )
            ),
            avsluttet = TestData.nyMetadataRow(
                utfoertAv = TestData.nyBrukerRow(type = BrukerType.SYSTEM, brukerId = "ARENA")
            )
        )
        repository.lagrePeriode(updatedPeriode.toPeriode())

        val lagretPeriode = repository.hentPeriodeForId(periode.periodeId)

        updatedPeriode shouldBeEqualTo lagretPeriode
    }

    "Oppdatere avsluttet periode med null avsluttet metadata" {
        val periode = TestData.nyAvsluttetPeriodeRow(identitetsnummer = TestData.fnr5)
        repository.lagrePeriode(periode.toPeriode())
        val updatedPeriode = periode.copy(
            startet = TestData.nyMetadataRow(
                tidspunkt = Instant.now().minus(Duration.ofDays(2)),
                utfoertAv = TestData.nyBrukerRow(type = BrukerType.UDEFINERT, brukerId = "XYZ"),
                kilde = "ANNEN_KILDE",
                aarsak = "ANNEN_AARSAK",
                tidspunktFraKilde = TestData.nyTidspunktFraKildeRow(
                    tidspunkt = Instant.now().minus(Duration.ofDays(1)),
                    avviksType = AvviksType.FORSINKELSE
                )
            ),
            avsluttet = null
        )
        repository.lagrePeriode(updatedPeriode.toPeriode())

        val lagretPeriode = repository.hentPeriodeForId(periode.periodeId)

        updatedPeriode shouldBeEqualTo lagretPeriode
    }

    "Lagre startede perioder i batch" {
        val periode1 = TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr6)
        val periode2 = TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr7)
        repository.lagreAllePerioder(sequenceOf(periode1.toPeriode(), periode2.toPeriode()))

        val lagretPeriode1 = repository.hentPeriodeForId(periode1.periodeId)
        val lagretPeriode2 = repository.hentPeriodeForId(periode2.periodeId)

        periode1 shouldBeEqualTo lagretPeriode1
        periode2 shouldBeEqualTo lagretPeriode2
    }

    "Lagre noen avsluttede perioder i batch" {
        val periode1 = TestData.nyStartetPeriodeRow(
            identitetsnummer = TestData.fnr8,
            startetMetadata = TestData.nyMetadataRow(
                tidspunkt = Instant.now().minus(Duration.ofDays(1)),
                utfoertAv = TestData.nyBrukerRow(brukerId = TestData.fnr8)
            )
        )
        val periode2 = TestData.nyStartetPeriodeRow(
            identitetsnummer = TestData.fnr9,
            startetMetadata = TestData.nyMetadataRow(
                tidspunkt = Instant.now().minus(Duration.ofDays(2)),
                utfoertAv = TestData.nyBrukerRow(brukerId = TestData.fnr9)
            )
        )
        val periode3 = TestData.nyStartetPeriodeRow(
            identitetsnummer = TestData.fnr10,
            startetMetadata = TestData.nyMetadataRow(
                tidspunkt = Instant.now().minus(Duration.ofDays(3)),
                utfoertAv = TestData.nyBrukerRow(type = BrukerType.VEILEDER, brukerId = TestData.navIdent2)
            )
        )
        val periode4 = periode1.copy(
            startet = TestData.nyMetadataRow(
                tidspunkt = Instant.now(),
                utfoertAv = TestData.nyBrukerRow(brukerId = TestData.fnr8),
                kilde = "NY_KILDE",
                aarsak = "NY_AARSAK",
                tidspunktFraKilde = TestData.nyTidspunktFraKildeRow(
                    tidspunkt = Instant.now(),
                    avviksType = AvviksType.RETTING
                )
            ),
            avsluttet = TestData.nyMetadataRow(
                utfoertAv = TestData.nyBrukerRow(type = BrukerType.SYSTEM, brukerId = "ARENA")
            )
        )
        val periode5 = periode2.copy(
            avsluttet = TestData.nyMetadataRow(
                utfoertAv = TestData.nyBrukerRow(type = BrukerType.SYSTEM, brukerId = "ARENA")
            )
        )
        val startedePerioder = sequenceOf(periode1.toPeriode(), periode2.toPeriode(), periode3.toPeriode())
        val avsluttedePerioder = sequenceOf(periode4.toPeriode(), periode5.toPeriode())
        repository.lagreAllePerioder(startedePerioder)
        repository.lagreAllePerioder(avsluttedePerioder)

        val lagretPeriode1 = repository.hentPeriodeForId(periode1.periodeId)
        val lagretPeriode2 = repository.hentPeriodeForId(periode2.periodeId)
        val lagretPeriode3 = repository.hentPeriodeForId(periode3.periodeId)
        val lagretPeriode4 = repository.hentPeriodeForId(periode4.periodeId)
        val lagretPeriode5 = repository.hentPeriodeForId(periode5.periodeId)

        periode4 shouldBeEqualTo lagretPeriode1
        periode5 shouldBeEqualTo lagretPeriode2
        periode3 shouldBeEqualTo lagretPeriode3
        periode4 shouldBeEqualTo lagretPeriode4
        periode5 shouldBeEqualTo lagretPeriode5
        lagretPeriode1!! shouldBeEqualTo lagretPeriode4
        lagretPeriode2!! shouldBeEqualTo lagretPeriode5
    }
})
