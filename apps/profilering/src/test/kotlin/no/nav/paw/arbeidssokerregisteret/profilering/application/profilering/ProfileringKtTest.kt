package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.ProfileringTestData.toInstant
import java.time.LocalDate

class ProfileringKtTest : FreeSpec({
    "En med alder på 61 år skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING}" {
        val sekstiEnAarSiden = LocalDate.now().minusYears(61)
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(
                foedselsdato = sekstiEnAarSiden,
                foedselsAar = sekstiEnAarSiden.year
            ),
            opplysninger = ProfileringTestData.standardOpplysninger()
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    }
    "En med alder på 17 år skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING}" {
        val syttenAarSiden = LocalDate.now().minusYears(17)
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(
                foedselsdato = syttenAarSiden,
                foedselsAar = syttenAarSiden.year
            ),
            opplysninger = ProfileringTestData.standardOpplysninger()
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    }
    "En med alder innenfor 18..59 og oppfyller krav til arbeidserfaring, utdanning, helsehinder og andre forhold blir profilert til ${ProfilertTil.ANTATT_GODE_MULIGHETER}" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(),
            ProfileringTestData.standardOpplysninger()
        ).profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
    }
    "En med 'helsehinder' skal gi ${ProfilertTil.OPPGITT_HINDRINGER}" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(),
            ProfileringTestData.standardOpplysninger(helse = Helse(JaNeiVetIkke.JA))
        ).profilertTil shouldBe ProfilertTil.OPPGITT_HINDRINGER
    }
    "En med 'andre forhold' skal gi ${ProfilertTil.OPPGITT_HINDRINGER}" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(),
            ProfileringTestData.standardOpplysninger(annet = Annet(JaNeiVetIkke.JA))
        ).profilertTil shouldBe ProfilertTil.OPPGITT_HINDRINGER
    }
    "En med 'utdanning bestaatt = nei' skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING} dersom nuskode > 2" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(),
            ProfileringTestData.standardOpplysninger(utdanning = Utdanning("3", JaNeiVetIkke.NEI, JaNeiVetIkke.JA))
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(),
            ProfileringTestData.standardOpplysninger(utdanning = Utdanning("2", JaNeiVetIkke.NEI, JaNeiVetIkke.JA))
        ).profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
    }
    "En med 'utdanning godkjent = nei' skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING} dersom nuskode > 2" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(),
            ProfileringTestData.standardOpplysninger(utdanning = Utdanning("3", JaNeiVetIkke.JA, JaNeiVetIkke.NEI))
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(),
            ProfileringTestData.standardOpplysninger(utdanning = Utdanning("2", JaNeiVetIkke.JA, JaNeiVetIkke.NEI))
        ).profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
    }
    "En med 'nus = 0' skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING}" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(),
            ProfileringTestData.standardOpplysninger(utdanning = Utdanning("0", JaNeiVetIkke.JA, JaNeiVetIkke.JA))
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    }
    "En med 'nus = 9' skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING}" {
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(),
            ProfileringTestData.standardOpplysninger(utdanning = Utdanning("9", JaNeiVetIkke.JA, JaNeiVetIkke.JA))
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    }
    "En med 'kilde = veilarbregistrering' og 6 mnd sammenhengende jobb skal gi ${ProfilertTil.ANTATT_GODE_MULIGHETER}" {
        val sendtInnTidspunkt = LocalDate.of(2020, 3, 16).toInstant()
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(
                arbeidsforhold = listOf(
                    arbeidsforhold(fra = "2019-09-15", til = "2020-03-15")
                )
            ),
            ProfileringTestData.standardOpplysninger(
                sendtInnTidspunkt = sendtInnTidspunkt,
                kilde = "veilarbregistrering"
            )
        ).profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
    }
    "En med 'kilde = veilarbregistrering' og jobb 1. i måneden 6 måneder på rad skal gi ${ProfilertTil.ANTATT_GODE_MULIGHETER}" {
        val sendtInnTidspunkt = LocalDate.of(2021, 12, 31).toInstant()
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(
                arbeidsforhold = listOf(
                    arbeidsforhold(fra = "2021-11-30", til = "2021-12-02"),
                    arbeidsforhold(fra = "2021-10-31", til = "2021-11-02"),
                    arbeidsforhold(fra = "2021-09-30", til = "2021-10-02"),
                    arbeidsforhold(fra = "2021-08-31", til = "2021-09-02"),
                    arbeidsforhold(fra = "2021-07-31", til = "2021-08-02"),
                    arbeidsforhold(fra = "2021-06-30", til = "2021-07-02")
                )
            ),
            ProfileringTestData.standardOpplysninger(
                sendtInnTidspunkt = sendtInnTidspunkt,
                kilde = "veilarbregistrering"
            )
        ).profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
    }
    "En med 'kilde = noe annet' og jobb 1. i måneden 6 måneder på rad skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING}" {
        val sendtInnTidspunkt = LocalDate.of(2021, 12, 31).toInstant()
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(
                arbeidsforhold = listOf(
                    arbeidsforhold(fra = "2021-11-30", til = "2021-12-02"),
                    arbeidsforhold(fra = "2021-10-31", til = "2021-11-02"),
                    arbeidsforhold(fra = "2021-09-30", til = "2021-10-02"),
                    arbeidsforhold(fra = "2021-08-31", til = "2021-09-02"),
                    arbeidsforhold(fra = "2021-07-31", til = "2021-08-02"),
                    arbeidsforhold(fra = "2021-06-30", til = "2021-07-02")
                )
            ),
            ProfileringTestData.standardOpplysninger(
                sendtInnTidspunkt = sendtInnTidspunkt,
                kilde = "noe annet"
            )
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    }
    "En med 'kilde = veilarbregistrering' og under 6 mnd sammenhengende jobb skal gi ${ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING}" {
        val sendtInnTidspunkt = LocalDate.of(2020, 3, 16).toInstant()
        profiler(
            ProfileringTestData.standardBrukerPersonInfo(
                arbeidsforhold = listOf(
                    arbeidsforhold(fra = "2020-01-01", til = "2020-03-15")
                )
            ),
            ProfileringTestData.standardOpplysninger(
                sendtInnTidspunkt = sendtInnTidspunkt,
                kilde = "veilarbregistrering"
            )
        ).profilertTil shouldBe ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
    }
    "En med 'kilde = noe annet' og som har vært i jobb i over et år skal gi ${ProfilertTil.ANTATT_GODE_MULIGHETER}" {
        val sendtInnTidspunkt = LocalDate.of(2021, 6, 21).toInstant()
        val resultat = profiler(
            ProfileringTestData.standardBrukerPersonInfo(
                arbeidsforhold = listOf(
                    arbeidsforhold(fra = "2018-08-01", til = null)
                )
            ),
            ProfileringTestData.standardOpplysninger(
                sendtInnTidspunkt = sendtInnTidspunkt,
                kilde = "noe annet"
            )
        )
        resultat.jobbetSammenhengendeSeksAvTolvSisteMnd shouldBe true
        resultat.profilertTil shouldBe ProfilertTil.ANTATT_GODE_MULIGHETER
    }
})