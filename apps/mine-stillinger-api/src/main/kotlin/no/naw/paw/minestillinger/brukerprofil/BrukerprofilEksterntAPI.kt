package no.naw.paw.minestillinger.brukerprofil

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import no.nav.paw.error.model.Data
import no.nav.paw.error.model.Response
import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.api.ApiStillingssoek
import no.naw.paw.minestillinger.api.domain
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
import no.naw.paw.minestillinger.api.vo.ApiTjenesteStatus
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.GRADERT_ADRESSE_GYLDIGHETS_PERIODE
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.LagretStillingsoek
import no.naw.paw.minestillinger.domain.Stillingssoek
import no.naw.paw.minestillinger.domain.TjenesteStatus
import no.naw.paw.minestillinger.domain.api
import no.naw.paw.minestillinger.domain.toTjenesteStatus
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import java.time.Instant.now
import kotlin.time.Duration

suspend fun BrukerprofilTjeneste.hentBrukerprofil(
    hentSøk: (BrukerId) -> List<Stillingssoek>,
    identitetsnummer: Identitetsnummer
): ApiBrukerprofil? {
    return suspendedTransactionAsync {
        hentBrukerProfil(identitetsnummer)
            ?.let { brukerprofil ->
                val søk = hentSøk(brukerprofil.id).map { søk -> søk.api() }
                brukerprofil.api().copy(stillingssoek = søk)
            }
    }.await()
        .also { apiBrukerprofil ->
            appLogger.trace("Returnerer brukerprofil: tjenestestatus=${apiBrukerprofil?.tjenestestatus}, antallSøk=${apiBrukerprofil?.stillingssoek?.size}")
        }
}

suspend fun BrukerprofilTjeneste.oppdaterProfilMedGradertAdresseDersomAktuelt(profil: BrukerProfil): BrukerProfil =
    if (profil.listeMedFlagg.tjenestestatus() != TjenesteStatus.KAN_IKKE_LEVERES &&
        profil.listeMedFlagg.tjenestestatus() != TjenesteStatus.OPT_OUT
    ) {
        hentAddresseBeskyttelseFlagg(
            brukerProfil = profil,
            tidspunkt = clock.now(),
            maxAlder = GRADERT_ADRESSE_GYLDIGHETS_PERIODE
        )
    } else profil

suspend fun BrukerprofilTjeneste.setTjenestatestatus(
    identitetsnummer: Identitetsnummer,
    tjenesteStatus: ApiTjenesteStatus
): Response<Unit> {
    return suspendedTransactionAsync {
        val brukerprofil = hentBrukerProfil(identitetsnummer)
            ?.let { profil ->
                if(tjenesteStatus == ApiTjenesteStatus.AKTIV) {
                    oppdaterProfilMedGradertAdresseDersomAktuelt(profil)
                } else profil
            }
            ?: brukerIkkeFunnet()
        val domainStatus = tjenesteStatus.toTjenesteStatus()
        val listMedFlag = brukerprofil.listeMedFlagg
        val gjeldendeStatus = listMedFlag.tjenestestatus()
        when {
            domainStatus == TjenesteStatus.KAN_IKKE_LEVERES -> oppdateringIkkeTillatt("Kan ikke sette tjenestestatus til KAN_IKKE_LEVERES manuelt")
            gjeldendeStatus == TjenesteStatus.KAN_IKKE_LEVERES -> oppdateringIkkeTillatt("Kan ikke oppdatere tjenestestatus fra KAN_IKKE_LEVERES")
            else -> {
                val tidspunkt = clock.now()
                val oppdateringer = beregnOppdateringAvFlaggFraAPI(
                    tidspunkt = tidspunkt,
                    gjeldendeStatus = gjeldendeStatus,
                    nyTjenestestatus = domainStatus
                )
                oppdaterFlagg(brukerprofil.id, oppdateringer)
                Data(Unit)
            }
        }
    }.await()
}

suspend fun BrukerprofilTjeneste.hentSøk(
    hentSøk: (BrukerId) -> List<Stillingssoek>,
    identitetsnummer: Identitetsnummer
): List<ApiStillingssoek> {
    return suspendedTransactionAsync {
        val brukerprofil = hentBrukerProfil(identitetsnummer) ?: brukerIkkeFunnet()
        hentSøk(brukerprofil.id).map { søk -> søk.api() }
    }.await()
}

suspend fun BrukerprofilTjeneste.hentLagretSøk(
    hentSøk: (BrukerId) -> List<LagretStillingsoek>,
    identitetsnummer: Identitetsnummer
): List<LagretStillingsoek> {
    return suspendedTransactionAsync {
        val brukerprofil = hentBrukerProfil(identitetsnummer) ?: brukerIkkeFunnet()
        hentSøk(brukerprofil.id)
    }.await()
}

suspend fun BrukerprofilTjeneste.lagreSøk(
    lagreSøk: (BrukerId, List<Stillingssoek>) -> Unit,
    identitetsnummer: Identitetsnummer,
    søk: List<ApiStillingssoek>
) {
    suspendedTransactionAsync {
        val brukerprofil = hentBrukerProfil(identitetsnummer) ?: brukerIkkeFunnet()
        if (!brukerprofil.tjenestenErAktiv) {
            throw BadRequestException("Kan ikke lagre søk for en bruker med inaktiv tjenestestatus")
        }
        lagreSøk(brukerprofil.id, søk.map { it.domain() })
    }.await()
}

fun brukerIkkeFunnet(): Nothing {
    throw NotFoundException(
        "Brukerprofil ikke funnet for gitt identitetsnummer"
    )
}