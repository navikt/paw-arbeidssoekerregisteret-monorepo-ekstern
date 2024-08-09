package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.Period

class EvaluerArbeidserfaringKtTest : StringSpec({

    "evaluerArbeidsErfaring skal returnere ${ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING} hvis kriteriene er møtt" {
        val marginForSammenhengendeJobb = 3
        val minimumsArbeidserfaring = Period.ofMonths(6)
        val periode = LocalDate.now().minusMonths(12)..<LocalDate.now()
        val arbeidsforhold = ProfileringTestData.arbeidsforhold()

        val result =
            evaluerArbeidsErfaring(
                marginForSammenhengendeJobb,
                minimumsArbeidserfaring,
                periode,
                listOf(arbeidsforhold)
            )

        result shouldBe setOf(ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING)
    }

    "evaluerArbeidsErfaring skal returnere ${ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING} for pågående jobb som har vart mer enn 6 måneder" {
        val marginForSammenhengendeJobb = 3
        val minimumsArbeidserfaring = Period.ofMonths(6)
        val periode = LocalDate.now().minusMonths(12)..<LocalDate.now()
        val arbeidsforhold = ProfileringTestData.arbeidsforhold(
            ansettelsesperiode = ProfileringTestData.ansettelsesperiode(
                fom = LocalDate.now().minusYears(7),
                tom = null
            )
        )

        val result =
            evaluerArbeidsErfaring(
                marginForSammenhengendeJobb,
                minimumsArbeidserfaring,
                periode,
                listOf(arbeidsforhold)
            )

        result shouldBe setOf(ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING)
    }

    "evaluerArbeidsErfaring skal returnere ${setOf(ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING)} når vi har en " +
            "jobbperiode som er lang nok og innenfor perioden" {
                val marginForSammenhengendeJobb = 3
                val minimumsArbeidserfaring = Period.ofMonths(6)
                val periode = LocalDate.now().minusMonths(12)..<LocalDate.now()
                val arbeidsforhold = ProfileringTestData.arbeidsforhold(
                    ansettelsesperiode = ProfileringTestData.ansettelsesperiode(
                            fom = LocalDate.now().minusMonths(7),
                            tom = LocalDate.now()
                        )
                    )

                val result =
                    evaluerArbeidsErfaring(
                        marginForSammenhengendeJobb,
                        minimumsArbeidserfaring,
                        periode,
                        listOf(arbeidsforhold)
                    )

                result shouldBe setOf(ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING)
            }

    "evaluerArbeidsErfaring skal slå sammen to arbeidsforhold hvis de er innenfor marginen" {
        val marginForSammenhengendeJobb = 3
        val minimumsArbeidserfaring = Period.ofMonths(6)
        val periode = LocalDate.now().minusMonths(12)..<LocalDate.now()
        val arbeidsforhold1 = ProfileringTestData.arbeidsforhold(
            ansettelsesperiode = ProfileringTestData.ansettelsesperiode(
                fom = LocalDate.now().minusMonths(12),
                tom = LocalDate.now().minusMonths(7)
            )
        )
        val arbeidsforhold2 = ProfileringTestData.arbeidsforhold(
            ansettelsesperiode = ProfileringTestData.ansettelsesperiode(
                fom = LocalDate.now().minusMonths(7).plusDays(1),
                tom = LocalDate.now().minusMonths(3)
            )
        )

        val result =
            evaluerArbeidsErfaring(
                marginForSammenhengendeJobb,
                minimumsArbeidserfaring,
                periode,
                listOf(arbeidsforhold1, arbeidsforhold2)
            )

        result shouldBe setOf(ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING)
    }

    "evaluerArbeidsErfaring skal returnere tomt set når perioden er for kort" {
        val antallDagerMellomToPerioder = 5
        val minimumsArbeidserfaring = Period.ofMonths(6)
        val periode = LocalDate.parse("2020-06-01")..<LocalDate.parse("2021-06-01")
        val arbeidsforhold = listOf(
            arbeidsforhold(fra = "2020-01-01", til = "2020-03-15"),
            arbeidsforhold(fra = "2020-03-18", til = "2020-03-31"),
            arbeidsforhold(fra = "2020-05-01", til = "2020-07-01"),
            arbeidsforhold(fra = "2020-07-02", til = "2020-11-28"),
            arbeidsforhold(fra = "2021-03-15", til = "2021-04-01"),
            arbeidsforhold(fra = "2021-04-03", til = "2021-06-02")
        )
        val sammenhengendePerioder = arbeidsforhold
            .flettSammenhengendeJobber(antallDagerMellomToPerioder)
            .map {
                it.tid.start.toString() to it.tid.endExclusive.toString()
            }
        sammenhengendePerioder shouldBe listOf(
            "2020-01-01" to "2020-03-31",
            "2020-05-01" to "2020-11-28",
            "2021-03-15" to "2021-06-02"
        )
        val lengsteSammenhengdeJobbPeriode = lengsteSammenhengendeInnenforPeriode(
            arbeidsforhold = arbeidsforhold,
            periode = periode,
            marginForSammenhengendeJobbDager = antallDagerMellomToPerioder
        )
        lengsteSammenhengdeJobbPeriode?.tid?.start.toString() shouldBe "2020-05-01"
        lengsteSammenhengdeJobbPeriode?.tid?.endExclusive.toString() shouldBe "2020-11-28"

        val faktiskPeriode = lengsteSammenhengdeJobbPeriode?.periodeInnenfor(periode)
        faktiskPeriode?.start.toString() shouldBe "2020-06-01"
        faktiskPeriode?.endExclusive.toString() shouldBe "2020-11-28"

        val result = evaluerArbeidsErfaring(
            antallDagerMellomToPerioder,
            minimumsArbeidserfaring,
            periode,
            arbeidsforhold
        )
        result shouldBe emptySet()
    }

    "evaluerArbeidsErfaring skal returnere tomt set når perioden er lang nok" {
        val antallDagerMellomToPerioder = 5
        val minimumsArbeidserfaring = Period.ofMonths(6)
        val periode = LocalDate.parse("2020-06-01")..<LocalDate.parse("2021-06-01")
        val arbeidsforhold = listOf(
            arbeidsforhold(fra = "2020-01-01", til = "2020-03-15"),
            arbeidsforhold(fra = "2020-03-18", til = "2020-03-31"),
            arbeidsforhold(fra = "2020-05-01", til = "2020-07-01"),
            arbeidsforhold(fra = "2020-07-02", til = "2020-12-01"),
            arbeidsforhold(fra = "2021-03-15", til = "2021-04-01"),
            arbeidsforhold(fra = "2021-04-03", til = "2021-06-02")
        )
        val sammenhengendePerioder = arbeidsforhold
            .flettSammenhengendeJobber(antallDagerMellomToPerioder)
            .map {
                it.tid.start.toString() to it.tid.endExclusive.toString()
            }
        sammenhengendePerioder shouldBe listOf(
            "2020-01-01" to "2020-03-31",
            "2020-05-01" to "2020-12-01",
            "2021-03-15" to "2021-06-02"
        )
        val lengsteSammenhengdeJobberIPeriode = lengsteSammenhengendeInnenforPeriode(
            arbeidsforhold = arbeidsforhold,
            periode = periode,
            marginForSammenhengendeJobbDager = antallDagerMellomToPerioder
        )
        lengsteSammenhengdeJobberIPeriode?.tid?.start.toString() shouldBe "2020-05-01"
        lengsteSammenhengdeJobberIPeriode?.tid?.endExclusive.toString() shouldBe "2020-12-01"

        val faktiskPeriode = lengsteSammenhengdeJobberIPeriode?.periodeInnenfor(periode)
        faktiskPeriode?.start.toString() shouldBe "2020-06-01"
        faktiskPeriode?.endExclusive.toString() shouldBe "2020-12-01"

        val result = evaluerArbeidsErfaring(
            antallDagerMellomToPerioder,
            minimumsArbeidserfaring,
            periode,
            arbeidsforhold
        )
        result shouldBe setOf(ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING)
    }
    "evaluerArbeidsErfaring skal håndtere pågående perioder" {
        val antallDagerMellomToPerioder = 5
        val minimumsArbeidserfaring = Period.ofMonths(6)
        val periode = LocalDate.parse("2020-06-01")..<LocalDate.parse("2021-06-01")
        val arbeidsforhold = listOf(
            arbeidsforhold(fra = "2017-04-01", til = null),
            arbeidsforhold(fra = "2016-03-18", til = "2017-03-31"),
            arbeidsforhold(fra = "2013-01-12", til = "2016-03-01")
        )
        val sammenhengendePerioder = arbeidsforhold
            .flettSammenhengendeJobber(antallDagerMellomToPerioder)
            .map {
                it.tid.start.toString() to it.tid.endExclusive.toString()
            }
        sammenhengendePerioder shouldBe listOf(
            "2016-03-18" to LocalDate.MAX.toString(),
            "2013-01-12" to "2016-03-01"
        )
        val lengsteSammenhengdeJobbPeriode = lengsteSammenhengendeInnenforPeriode(
            arbeidsforhold = arbeidsforhold,
            periode = periode,
            marginForSammenhengendeJobbDager = antallDagerMellomToPerioder
        )
        lengsteSammenhengdeJobbPeriode?.tid?.start.toString() shouldBe "2016-03-18"
        lengsteSammenhengdeJobbPeriode?.tid?.endExclusive.toString() shouldBe LocalDate.MAX.toString()

        val faktiskPeriode = lengsteSammenhengdeJobbPeriode?.periodeInnenfor(periode)
        faktiskPeriode?.start.toString() shouldBe "2020-06-01"
        faktiskPeriode?.endExclusive.toString() shouldBe "2021-06-01"

        val result = evaluerArbeidsErfaring(
            antallDagerMellomToPerioder,
            minimumsArbeidserfaring,
            periode,
            arbeidsforhold
        )
        result shouldBe setOf(ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING)
    }
    "evaluerArbeidsErfaring skal håndtere flere pågående perioder" {
        val antallDagerMellomToPerioder = 5
        val minimumsArbeidserfaring = Period.ofMonths(6)
        val periode = LocalDate.parse("2020-06-01")..<LocalDate.parse("2021-06-01")
        val arbeidsforhold = listOf(
            arbeidsforhold(fra = "2017-04-01", til = null),
            arbeidsforhold(fra = "2016-03-18", til = "2017-03-31"),
            arbeidsforhold(fra = "2013-01-12", til = null)
        )
        val sammenhengendePerioder = arbeidsforhold
            .flettSammenhengendeJobber(antallDagerMellomToPerioder)
            .map {
                it.tid.start.toString() to it.tid.endExclusive.toString()
            }
        sammenhengendePerioder shouldBe listOf(
            "2013-01-12" to LocalDate.MAX.toString()
        )
        val lengsteSammenhengdeJobbPeriode = lengsteSammenhengendeInnenforPeriode(
            arbeidsforhold = arbeidsforhold,
            periode = periode,
            marginForSammenhengendeJobbDager = antallDagerMellomToPerioder
        )
        lengsteSammenhengdeJobbPeriode?.tid?.start.toString() shouldBe "2013-01-12"
        lengsteSammenhengdeJobbPeriode?.tid?.endExclusive.toString() shouldBe LocalDate.MAX.toString()

        val faktiskPeriode = lengsteSammenhengdeJobbPeriode?.periodeInnenfor(periode)
        faktiskPeriode?.start.toString() shouldBe "2020-06-01"
        faktiskPeriode?.endExclusive.toString() shouldBe "2021-06-01"

        val result = evaluerArbeidsErfaring(
            antallDagerMellomToPerioder,
            minimumsArbeidserfaring,
            periode,
            arbeidsforhold
        )
        result shouldBe setOf(ProfileringsTagger.OPPFYLLER_KRAV_TIL_ARBEIDSERFARING)
    }
})