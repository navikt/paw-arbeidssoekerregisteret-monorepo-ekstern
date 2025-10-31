package no.naw.paw.minestillinger.brukerprofil

import no.nav.paw.model.Identitetsnummer
import no.nav.paw.pdl.client.PdlClient
import no.naw.paw.minestillinger.Clock
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
import no.naw.paw.minestillinger.brukerprofil.flagg.erFremdelesGyldig
import no.naw.paw.minestillinger.db.ops.skrivFlaggTilDB
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.BrukerProfilerUtenFlagg
import no.naw.paw.minestillinger.domain.PeriodeId
import no.naw.paw.minestillinger.domain.Profilering
import no.naw.paw.minestillinger.domain.ProfileringResultat
import no.naw.paw.minestillinger.domain.TjenesteStatus
import no.naw.paw.minestillinger.domain.medFlagg
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Duration
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

    fun hentBrukerProfil(identitetsnummer: Identitetsnummer): BrukerProfil? {
        val brukerProfilerUtenFlagg = hentBrukerprofilUtenFlagg(identitetsnummer) ?: return null
        val profileringsFlagg = genererProfileringsFlagg(brukerProfilerUtenFlagg.arbeidssoekerperiodeId)
        val erITestGruppenFlagg = genererErITestGruppenFlagg(abTestingRegex, identitetsnummer)
        val flaggFraDatabasen = hentFlagg(brukerProfilerUtenFlagg.id)
        val gjeldeneFlagg = ListeMedFlagg(flaggFraDatabasen) + erITestGruppenFlagg + profileringsFlagg
        return brukerProfilerUtenFlagg.medFlagg(gjeldeneFlagg)
    }

    suspend fun hentAddresseBeskyttelseFlagg(
        brukerProfil: BrukerProfil,
        tidspunkt: Instant,
        maxAlder: Duration
    ): BrukerProfil {
        return brukerProfil.listeMedFlagg[HarGradertAdresseFlaggtype].let { flagg ->
            if (flagg?.erFremdelesGyldig(tidspunkt, maxAlder) ?: false) {
                brukerProfil
            } else {
                val harGradertAdresseNå = pdlClient.harBeskyttetAdresse(brukerProfil.identitetsnummer)
                val gradertAdresseFlagg = HarGradertAdresseFlagg(
                    verdi = harGradertAdresseNå,
                    tidspunkt = tidspunkt
                )
                oppdaterFlagg(
                    brukerProfil.id, OppdateringAvFlagg(
                        nyeOgOppdaterteFlagg = listOf(gradertAdresseFlagg),
                        søkSkalSlettes = harGradertAdresseNå
                    )
                )
                brukerProfil.copy(listeMedFlagg = brukerProfil.listeMedFlagg.addOrUpdate(gradertAdresseFlagg))
            }
        }
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
    }
}
