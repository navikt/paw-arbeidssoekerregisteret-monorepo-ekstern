package no.nav.paw.arbeidssoekerregisteret.eksternt.api.repositories

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.test.ApplicationTestContext
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.test.TestData
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.test.shouldBeEqualTo
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.TimeUtils.getMaxDateForDatabaseStorage
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils.toInstant
import java.time.Duration
import java.time.Instant

class ArbeidssokerperiodeRepositoryTest : FreeSpec({
    with(ApplicationTestContext.withRealDataAccess()) {

        "Opprett og hent en periode" {
            val periodeRow = TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr1)
            periodeRepository.opprettPeriode(periodeRow)
            val retrievedPeriodeRow = periodeRepository.hentPeriode(periodeRow.periodeId)

            periodeRow shouldBeEqualTo retrievedPeriodeRow
        }

        "Hent en periode for et gitt identitetsnummer" {
            val periodeRow = TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr2)
            periodeRepository.opprettPeriode(periodeRow)
            val retrievedPeriodeRows = periodeRepository.finnPerioder(TestData.identitetsnummer2)

            retrievedPeriodeRows.size shouldBeExactly 1
            periodeRow shouldBeEqualTo retrievedPeriodeRows[0]
        }

        "Oppdater periode med avsluttet tidspunkt" {
            val periodeRow = TestData.nyAvsluttetPeriodeRow(identitetsnummer = TestData.fnr3)
            periodeRepository.opprettPeriode(periodeRow)
            val updatedPeriodeRow = periodeRow.copy(avsluttet = Instant.now().minus(Duration.ofDays(2)))
            periodeRepository.oppdaterPeriode(updatedPeriodeRow)
            val retrievedPeriodeRow = periodeRepository.hentPeriode(periodeRow.periodeId)

            updatedPeriodeRow shouldBeEqualTo retrievedPeriodeRow
        }

        "Oppdater periode uten avsluttet tidspunkt med nytt avsluttet tidspunkt" {
            val periodeRow = TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr4)
            periodeRepository.opprettPeriode(periodeRow)
            val updatedPeriodeRow = periodeRow.copy(avsluttet = Instant.now().minus(Duration.ofDays(2)))
            periodeRepository.oppdaterPeriode(updatedPeriodeRow)
            val retrievedPeriodeRow = periodeRepository.hentPeriode(periodeRow.periodeId)

            updatedPeriodeRow shouldBeEqualTo retrievedPeriodeRow
        }

        "Oppdater fødselsnummer på periode" {
            val periodeRow = TestData.nyStartetPeriodeRow(identitetsnummer = TestData.fnr5)
            periodeRepository.opprettPeriode(periodeRow)
            val updatedPeriodeRow = periodeRow.copy(identitetsnummer = TestData.fnr6)
            periodeRepository.oppdaterPeriode(updatedPeriodeRow)
            val retrievedPeriodeRow = periodeRepository.hentPeriode(periodeRow.periodeId)

            updatedPeriodeRow shouldBeEqualTo retrievedPeriodeRow
        }

        "Sletter perioder eldre enn 3 år pluss inneværende år" {
            val startet = getMaxDateForDatabaseStorage().minusDays(30)
            val avsluttet = getMaxDateForDatabaseStorage()
            val periodeRows = listOf(
                TestData.nyAvsluttetPeriodeRow(
                    identitetsnummer = TestData.fnr7,
                    startet = startet.toInstant(),
                    avsluttet = avsluttet.plusDays(1).toInstant()
                ),
                TestData.nyAvsluttetPeriodeRow(
                    identitetsnummer = TestData.fnr8,
                    startet = startet.toInstant(),
                    avsluttet = avsluttet.minusDays(1).toInstant()
                ),
            )
            periodeRows.forEach { periodeRepository.opprettPeriode(it) }
            periodeRepository.slettDataEldreEnnDatoFraDatabase(getMaxDateForDatabaseStorage().toInstant())

            val retrievedPerioder = periodeRows.mapNotNull { periodeRepository.hentPeriode(it.periodeId) }
            retrievedPerioder.size shouldBe 1
        }
    }
})
