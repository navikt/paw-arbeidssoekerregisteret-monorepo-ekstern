package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.ProfileringsTagger.*
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo
import java.time.Instant
import java.time.Period
import java.time.ZoneId

private const val MARGIN_FOR_SAMMENHENGENDE_JOBB_DAGER = 3
private const val MINIMUM_SAMMENHENGENDE_JOBB_MND = 6
private const val LENGDE_PAA_PERIODE_SOM_SKAL_SJEKKES_AAR = 1L

fun OpplysningerOmArbeidssoeker.sendtInnAvVeilarb() = sendtInnAv.kilde == "veilarbregistrering"
fun profiler(
    personInfo: PersonInfo,
    opplysninger: OpplysningerOmArbeidssoeker,
    veilarbModus: Boolean = opplysninger.sendtInnAvVeilarb()
): Profilering {
    val tidspunktFraKilde = opplysninger.sendtInnAv.tidspunkt
    val profileringTidspunkt = Instant.now()
    val sendtInnDato = tidspunktFraKilde.atZone(ZoneId.systemDefault()).toLocalDate()
    val alder = alderVedTidspunkt(tidspunktFraKilde, personInfo)
    val evalueringer =
                evaluerAnnet(opplysninger.annet) +
                evaluerAlder(alder) +
                evaluerHelse(opplysninger.helse) +
                evaluerUtdanning(opplysninger.utdanning) +
                if (veilarbModus)
                    veilarbHarJobbetSammenhengendeSeksAvTolvSisteManeder(
                        sendtInnDato,
                        personInfo.arbeidsforhold,
                    )
                else evaluerArbeidsErfaring(
                    MARGIN_FOR_SAMMENHENGENDE_JOBB_DAGER,
                    Period.ofMonths(MINIMUM_SAMMENHENGENDE_JOBB_MND),
                    sendtInnDato.minusYears(LENGDE_PAA_PERIODE_SOM_SKAL_SJEKKES_AAR)..<sendtInnDato,
                    personInfo.arbeidsforhold
                )


    fun resultat(profilering: ProfilertTil) =
        profileringsResultat(
            alder = alder,
            opplysninger = opplysninger,
            profilering = profilering,
            harJobbetSammenhengende6AvSiste12Mnd = evalueringer.contains(OPPFYLLER_KRAV_TIL_ARBEIDSERFARING),
            profileringTidspunkt = profileringTidspunkt,
            tidspunktFraKilde = tidspunktFraKilde
        )
    return when {
        evalueringer.anbefalerArbeidsevnevudering() -> resultat(ProfilertTil.OPPGITT_HINDRINGER)
        evalueringer.anbefalerStandardInnsats() -> resultat(ProfilertTil.ANTATT_GODE_MULIGHETER)
        else -> resultat(ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING)

    }
}

fun Set<ProfileringsTagger>.anbefalerArbeidsevnevudering(): Boolean =
    contains(HELSETILSTAND_HINDRER_ARBEID) ||
            contains(ANDRE_FORHOLD_HINDRER_ARBEID)

fun Set<ProfileringsTagger>.anbefalerStandardInnsats(): Boolean =
    contains(ALDER_INDIKERER_STANDARD_INNSATS) &&
            contains(OPPFYLLER_KRAV_TIL_ARBEIDSERFARING) &&
            contains(HAR_BESTAATT_GODKJENT_UTDANNING) &&
            !contains(HELSETILSTAND_HINDRER_ARBEID) &&
            !contains(ANDRE_FORHOLD_HINDRER_ARBEID)




