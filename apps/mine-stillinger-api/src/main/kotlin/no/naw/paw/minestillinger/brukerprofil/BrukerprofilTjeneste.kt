package no.naw.paw.minestillinger.brukerprofil

import no.nav.paw.model.Identitetsnummer
import no.nav.paw.pdl.client.PdlClient
import no.naw.paw.minestillinger.Clock
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.GRADERT_ADRESSE_GYLDIGHETS_PERIODE
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.harBeskyttetAdresse
import no.naw.paw.minestillinger.brukerprofil.flagg.ErITestGruppenFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.Flagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGodeMuligheterFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGodeMuligheterFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGradertAdresseFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.HarGradertAdresseFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.LagretFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.ListeMedFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.OppdateringAvFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.erFremdelesGyldig
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.BrukerProfilerUtenFlagg
import no.naw.paw.minestillinger.domain.PeriodeId
import no.naw.paw.minestillinger.domain.Profilering
import no.naw.paw.minestillinger.domain.ProfileringResultat
import no.naw.paw.minestillinger.domain.medFlagg
import java.time.Instant

class BrukerprofilTjeneste(
    val pdlClient: PdlClient,
    val hentBrukerprofilUtenFlagg: (Identitetsnummer) -> BrukerProfilerUtenFlagg?,
    val skrivFlagg: (BrukerId, Iterable<LagretFlagg>) -> Unit,
    val hentFlagg: (BrukerId) -> List<Flagg>,
    val hentProfilering: (PeriodeId) -> Profilering?,
    val slettAlleSøk: (BrukerId) -> Unit,
    val abTestingRegex: Regex,
    val clock: Clock
) {

    suspend fun hentBrukerProfil(identitetsnummer: Identitetsnummer): BrukerProfil? {
        val tidspunkt = clock.now()
        val brukerProfilerUtenFlagg = hentBrukerprofilUtenFlagg(identitetsnummer) ?: return null
        val flagg = hentFlagg(brukerProfilerUtenFlagg.id)
            .let(::ListeMedFlagg)
            .let { flagg ->
                val gradertAdresseGyldig = flagg[HarGradertAdresseFlaggtype]
                    ?.erFremdelesGyldig(
                        tidspunkt = tidspunkt,
                        gydlighetsperiode = GRADERT_ADRESSE_GYLDIGHETS_PERIODE
                    ) ?: false
                if (gradertAdresseGyldig) {
                    flagg
                } else {
                    val harGradertAdresseNå = pdlClient.harBeskyttetAdresse(identitetsnummer)
                    val oppdatering = OppdateringAvFlagg(
                        nyeOgOppdaterteFlagg = listOf(HarGradertAdresseFlagg(harGradertAdresseNå, tidspunkt)),
                        søkSkalSlettes = harGradertAdresseNå
                    )
                    oppdaterFlagg(brukerId = brukerProfilerUtenFlagg.id, oppdatering = oppdatering)
                    flagg.addOrUpdate(*oppdatering.nyeOgOppdaterteFlagg.toTypedArray())
                }
            }
        val profileringsFlagg = genererProfileringsFlagg(brukerProfilerUtenFlagg.arbeidssoekerperiodeId)
        val erITestGruppenFlagg = genererErITestGruppenFlagg(abTestingRegex, identitetsnummer)
        val alleFlagg = flagg.addOrUpdate(erITestGruppenFlagg, profileringsFlagg)
        appLogger.info("Flagg: ${alleFlagg.map { it.debug() }.joinToString(", ")}")
        return brukerProfilerUtenFlagg.medFlagg(alleFlagg)
    }

    fun genererProfileringsFlagg(periodeId: PeriodeId): HarGodeMuligheterFlagg {
        return hentProfilering(periodeId)
            ?.let { profilering ->
                val godeMuligheter = profilering.profileringResultat == ProfileringResultat.ANTATT_GODE_MULIGHETER
                HarGodeMuligheterFlaggtype.flagg(verdi = godeMuligheter, tidspunkt = profilering.profileringTidspunkt)
            } ?: HarGodeMuligheterFlaggtype.flagg(verdi = false, tidspunkt = Instant.EPOCH)
    }

    fun genererErITestGruppenFlagg(regex: Regex, identitetsnummer: Identitetsnummer): ErITestGruppenFlagg {
        return ErITestGruppenFlagg(
            verdi = sjekkABTestingGruppe(regex, identitetsnummer),
            tidspunkt = clock.now()
        )
    }

    fun oppdaterFlagg(brukerId: BrukerId, oppdatering: OppdateringAvFlagg) {
        skrivFlagg(brukerId, oppdatering.nyeOgOppdaterteFlagg)
        if (oppdatering.søkSkalSlettes) {
            slettAlleSøk(brukerId)
        }
        appLogger.info("Oppdaterte flagg: $oppdatering")
    }
}

fun oppdaterMedGradertAdresse(
    tidspunkt: Instant,
    gjeldeneFlagg: ListeMedFlagg,
): OppdateringAvFlagg {
    val nyeEllerOppdaterteFlagg = listOfNotNull(
        HarGradertAdresseFlagg(true, tidspunkt),
        if (gjeldeneFlagg[TjenestenErAktivFlaggtype]?.verdi == true) TjenestenErAktivFlagg(false, tidspunkt) else null
    )
    return OppdateringAvFlagg(
        nyeOgOppdaterteFlagg = nyeEllerOppdaterteFlagg,
        søkSkalSlettes = true
    )
}

