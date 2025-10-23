package no.naw.paw.minestillinger.brukerprofil

import no.nav.paw.model.Identitetsnummer
import no.nav.paw.pdl.client.PdlClient
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.BrukerProfilerUtenFlagg
import no.naw.paw.minestillinger.domain.ErITestGruppen
import no.naw.paw.minestillinger.domain.ErITestGruppenFlagg
import no.naw.paw.minestillinger.domain.FlaggListe
import no.naw.paw.minestillinger.domain.FlaggVerdi
import no.naw.paw.minestillinger.domain.HarGodeMuligheterFlagg
import no.naw.paw.minestillinger.domain.HarGodeMuligheter
import no.naw.paw.minestillinger.domain.HarGradertAdresse
import no.naw.paw.minestillinger.domain.HarGradertAdresseFlagg
import no.naw.paw.minestillinger.domain.PeriodeId
import no.naw.paw.minestillinger.domain.Profilering
import no.naw.paw.minestillinger.domain.ProfileringResultat
import no.naw.paw.minestillinger.domain.TjenestenErAktiv
import no.naw.paw.minestillinger.domain.TjenestenErAktivFlagg
import no.naw.paw.minestillinger.domain.medFlagg
import java.time.Instant

class BrukerprofilTjeneste(
    val pdlClient: PdlClient,
    val hentBrukerprofilUtenFlagg: (Identitetsnummer) -> BrukerProfilerUtenFlagg?,
    val skrivFlagg: (BrukerId, FlaggListe) -> Unit,
    val hentFlagg: (BrukerId) -> List<FlaggVerdi>,
    val hentProfilering: (PeriodeId) -> Profilering?,
    val slettAlleSøk: (BrukerId) -> Unit,
) {
    private val flaggIkkeLagretDirekte = setOf(
        HarGodeMuligheterFlagg,
        ErITestGruppenFlagg
    )

    suspend fun hentBrukerProfil(identitetsnummer: Identitetsnummer): BrukerProfil? {
        val tidspunkt = Instant.now()
        val brukerProfilerUtenFlagg = hentBrukerprofilUtenFlagg(identitetsnummer) ?: return null
        val flagg = hentFlagg(brukerProfilerUtenFlagg.id)
            .let(::FlaggListe)
            .let { flagg ->
                val gradertAdresseGyldig = flagg[HarGradertAdresseFlagg]
                    .erGyldig(
                        tidspunkt = tidspunkt,
                        gyldighetsperiode = GRADERT_ADRESSE_GYLDIGHETS_PERIODE
                    )
                if (gradertAdresseGyldig) {
                    flagg
                } else {
                    val harBeskyttetAdresseNå = pdlClient.harBeskyttetAdresse(identitetsnummer)
                    if (harBeskyttetAdresseNå) {
                        oppdaterMedGradertAdresse(
                            tidspunkt = tidspunkt,
                            gjeldeneFlagg = flagg
                        ).let { oppdatering ->
                            if (oppdatering.søkSkalSlettes) {
                                slettAlleSøk(brukerProfilerUtenFlagg.id)
                            }
                            flagg.addOrUpdate(*oppdatering.nyeOgOppdaterteFlagg.toTypedArray())
                        }
                    } else {
                        flagg.addOrUpdate(HarGradertAdresse(false, tidspunkt))
                    }
                }
            }
        val profileringsFlagg = genererProfileringsFlagg(brukerProfilerUtenFlagg.arbeidssoekerperiodeId)
        val erITestGruppenFlagg = genererErITestGruppenFlagg(identitetsnummer)
        val alleFlagg = flagg.addOrUpdate(erITestGruppenFlagg, profileringsFlagg)
        return brukerProfilerUtenFlagg.medFlagg(alleFlagg)
    }

    fun oppdaterFlagg(brukerId: BrukerId, flaggListe: FlaggListe) {
        val skalLagres = flaggListe
            .filterNot { flagg -> flagg.navn in flaggIkkeLagretDirekte }
            .let(::FlaggListe)
        skrivFlagg(brukerId, skalLagres)
    }

    fun genererProfileringsFlagg(periodeId: PeriodeId): HarGodeMuligheter {
        return hentProfilering(periodeId)
            ?.let { profilering ->
                val godeMuligheter = profilering?.profileringResultat == ProfileringResultat.ANTATT_GODE_MULIGHETER
                HarGodeMuligheterFlagg.flagg(verdi = godeMuligheter, tidspunkt = profilering.profileringTidspunkt)
            } ?: HarGodeMuligheterFlagg.flagg(verdi = false, tidspunkt = Instant.EPOCH)
    }

    fun genererErITestGruppenFlagg(identitetsnummer: Identitetsnummer): ErITestGruppen {
        return ErITestGruppen(
            verdi = sjekkABTestingGruppe(identitetsnummer),
            tidspunkt = Instant.now()
        )
    }

    fun oppdaterFlagg(brukerId: BrukerId, oppdatering: OppdateringAvStatus) {
        skrivFlagg(brukerId, FlaggListe(oppdatering.nyeOgOppdaterteFlagg))
        if (oppdatering.søkSkalSlettes) {
            slettAlleSøk(brukerId)
        }
    }
}

fun oppdaterMedGradertAdresse(
    tidspunkt: Instant,
    gjeldeneFlagg: FlaggListe
): OppdateringAvStatus {
    val nyeEllerOppdaterteFlagg = listOfNotNull(
        HarGradertAdresse(true, tidspunkt),
        if (gjeldeneFlagg[TjenestenErAktivFlagg]?.verdi == true) TjenestenErAktiv(false, tidspunkt) else null
    )
    return OppdateringAvStatus(
        nyeOgOppdaterteFlagg = nyeEllerOppdaterteFlagg,
        søkSkalSlettes = true
    )
}

data class OppdateringAvStatus(
    val nyeOgOppdaterteFlagg: List<FlaggVerdi>,
    val søkSkalSlettes: Boolean
)