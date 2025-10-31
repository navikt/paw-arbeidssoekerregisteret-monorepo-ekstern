package no.naw.paw.minestillinger.brukerprofil

import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.api.ApiStillingssoek
import no.naw.paw.minestillinger.api.domain
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
import no.naw.paw.minestillinger.api.vo.ApiTjenesteStatus
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.LagretStillingsoek
import no.naw.paw.minestillinger.domain.Stillingssoek
import no.naw.paw.minestillinger.domain.api
import no.naw.paw.minestillinger.domain.toTjenesteStatus
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync

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
    }.await().also { apiBrukerprofil ->
        appLogger.trace("Returnerer brukerprofil: tjenestestatus=${apiBrukerprofil?.tjenestestatus}, antallSøk=${apiBrukerprofil?.stillingssoek?.size}")
    }
}

suspend fun BrukerprofilTjeneste.setTjenestatestatus(
    identitetsnummer: Identitetsnummer,
    tjenesteStatus: ApiTjenesteStatus
) {
    suspendedTransactionAsync {
        val brukerprofil = hentBrukerProfil(identitetsnummer) ?: brukerIkkeFunnet()
        val domainStatus = tjenesteStatus.toTjenesteStatus()
        val oppdateringer = brukerprofil.listeMedFlagg
            .beregnOppdateringAvFlaggFraAPI(domainStatus)
        oppdaterFlagg(brukerprofil.id, oppdateringer)
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