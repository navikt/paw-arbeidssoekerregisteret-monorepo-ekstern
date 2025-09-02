package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo

class UtvidetProfileringsTest : FreeSpec({
    "Profilering" - {
        listOf(
            test(
                "Er 30 år og har jobbet sammenhengende i 6 av de siste 12 månedene",
                personInfo(
                    arbeidsforhold(fra = "2020-01-01", til = "2020-03-15"),
                    arbeidsforhold(fra = "2020-03-15", til = "2020-04-01"),
                    arbeidsforhold(fra = "2020-04-02", til = "2020-07-01"),
                    foedselsdato = "1990-02-27"
                ),
                opplysninger(
                    sendtIn = "2020-07-02",
                    utdanning = utdanning(
                        bestaatt = JaNeiVetIkke.JA,
                        godkjent = JaNeiVetIkke.JA
                    ),
                    helseHindrerArbeid = JaNeiVetIkke.NEI,
                    annetHindrerArbeid = JaNeiVetIkke.NEI
                ),
                forventetAlder = 30,
                forventetHarJobbetSammenhengendeSiste12Mnd = true,
                forventetProfilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER
            ),
            test(
                "Er 30 år og har ikke jobbet sammenhengende i 6 av de siste 12 månedene",
                personInfo(
                    arbeidsforhold(fra = "2018-01-01", til = "2019-08-15"),
                    arbeidsforhold(fra = "2020-03-15", til = "2020-04-01"),
                    arbeidsforhold(fra = "2020-04-02", til = "2020-07-01"),
                    foedselsdato = "1990-02-27"
                ),
                opplysninger(
                    sendtIn = "2020-07-02",
                    utdanning = utdanning(
                        bestaatt = JaNeiVetIkke.JA,
                        godkjent = JaNeiVetIkke.JA
                    ),
                    helseHindrerArbeid = JaNeiVetIkke.NEI,
                    annetHindrerArbeid = JaNeiVetIkke.NEI
                ),
                forventetAlder = 30,
                forventetHarJobbetSammenhengendeSiste12Mnd = false,
                forventetProfilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
            ),
            test("Er 62 år og har jobbet sammenhende i 6 av de 12 siste månedene",
                personInfo(
                    arbeidsforhold(fra = "2020-01-01", til = "2020-03-15"),
                    arbeidsforhold(fra = "2020-03-15", til = "2020-04-01"),
                    arbeidsforhold(fra = "2020-04-02", til = "2020-07-01"),
                    foedselsdato = "1958-02-27"
                ),
                opplysninger(
                    sendtIn = "2020-07-02",
                    utdanning = utdanning(
                        bestaatt = JaNeiVetIkke.JA,
                        godkjent = JaNeiVetIkke.JA
                    ),
                    helseHindrerArbeid = JaNeiVetIkke.NEI,
                    annetHindrerArbeid = JaNeiVetIkke.NEI
                ),
                forventetAlder = 62,
                forventetHarJobbetSammenhengendeSiste12Mnd = true,
                forventetProfilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
            ),
            test("Er 59 år og har jobbet sammenhende i 12 av de 12 siste månedene, er fremdeles i jobb",
                personInfo(
                    arbeidsforhold(fra = "2010-01-01", til = null),
                    arbeidsforhold(fra = "2008-03-15", til = "2009-04-01"),
                    arbeidsforhold(fra = "2000-04-02", til = "2006-07-01"),
                    foedselsdato = "1961-02-27"
                ),
                opplysninger(
                    sendtIn = "2020-07-02",
                    utdanning = utdanning(
                        bestaatt = JaNeiVetIkke.JA,
                        godkjent = JaNeiVetIkke.JA
                    ),
                    helseHindrerArbeid = JaNeiVetIkke.NEI,
                    annetHindrerArbeid = JaNeiVetIkke.NEI
                ),
                forventetAlder = 59,
                forventetHarJobbetSammenhengendeSiste12Mnd = true,
                forventetProfilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER
            ),
            test("Er 59 år og har ikke jobbet sammenhende i 6 av de 12 siste månedene, er fremdeles i jobb",
                personInfo(
                    arbeidsforhold(fra = "2020-07-01", til = null),
                    arbeidsforhold(fra = "2020-06-01", til = "2020-06-30"),
                    arbeidsforhold(fra = "2020-05-02", til = "2029-04-15"),
                    arbeidsforhold(fra = "2010-05-02", til = "2019-07-15"),
                    foedselsdato = "1961-02-27"
                ),
                opplysninger(
                    sendtIn = "2020-07-02",
                    utdanning = utdanning(
                        bestaatt = JaNeiVetIkke.JA,
                        godkjent = JaNeiVetIkke.JA
                    ),
                    helseHindrerArbeid = JaNeiVetIkke.NEI,
                    annetHindrerArbeid = JaNeiVetIkke.NEI
                ),
                forventetAlder = 59,
                forventetHarJobbetSammenhengendeSiste12Mnd = false,
                forventetProfilertTil = ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING
            )
        ).map { (beskrivelse, personInfo, opplysninger, forventetAlder, forventetHarJobbetSammenhengendeSiste12Mnd, forventetProfilertTil) ->
            beskrivelse {
                val profilering = profiler(
                    personInfo = personInfo,
                    opplysninger = opplysninger
                )
                profilering.profilertTil shouldBe forventetProfilertTil
                profilering.alder shouldBe forventetAlder
                profilering.jobbetSammenhengendeSeksAvTolvSisteMnd shouldBe forventetHarJobbetSammenhengendeSiste12Mnd
            }
        }
    }
})

fun test(
    beskrivelse: String,
    personInfo: PersonInfo,
    opplysninger: OpplysningerOmArbeidssoeker,
    forventetAlder: Int,
    forventetHarJobbetSammenhengendeSiste12Mnd: Boolean,
    forventetProfilertTil: ProfilertTil
): ProfileringsTest = ProfileringsTest(
    beskrivelse = beskrivelse,
    personInfo = personInfo,
    opplysninger = opplysninger,
    forventetAlder = forventetAlder,
    forventetHarJobbetSammenhengendeSiste12Mnd = forventetHarJobbetSammenhengendeSiste12Mnd,
    forventetProfilertTil = forventetProfilertTil
)


data class ProfileringsTest(
    val beskrivelse: String,
    val personInfo: PersonInfo,
    val opplysninger: OpplysningerOmArbeidssoeker,
    val forventetAlder: Int,
    val forventetHarJobbetSammenhengendeSiste12Mnd: Boolean,
    val forventetProfilertTil: ProfilertTil
)