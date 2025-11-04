package no.naw.paw.minestillinger.brukerprofil

import no.nav.paw.error.model.Data
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.error.model.Response
import no.nav.paw.error.model.flatMap
import no.nav.paw.error.model.map
import no.nav.paw.model.Identitetsnummer
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
import no.naw.paw.minestillinger.api.vo.ApiTjenesteStatus
import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.brukerprofil.beskyttetadresse.ADRESSEBESKYTTELSE_GYLDIGHETS_PERIODE
import no.naw.paw.minestillinger.brukerprofil.flagg.HarBruktTjenestenFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.OppdateringAvFlagg
import no.naw.paw.minestillinger.brukerprofil.flagg.OptOutFlaggtype
import no.naw.paw.minestillinger.brukerprofil.flagg.TjenestenErAktivFlaggtype
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.BrukerProfil
import no.naw.paw.minestillinger.domain.Stillingssoek
import no.naw.paw.minestillinger.domain.api
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync

suspend fun BrukerprofilTjeneste.hentApiBrukerprofil(
    hentSøk: (BrukerId) -> List<Stillingssoek>,
    identitetsnummer: Identitetsnummer
): ApiBrukerprofil? {
    return suspendedTransactionAsync {
        hentLokalBrukerProfilEllerNull(identitetsnummer)
            ?.let { brukerprofil ->
                val søk = hentSøk(brukerprofil.id).map { søk -> søk.api() }
                brukerprofil.api().copy(stillingssoek = søk)
            }
    }.await()
        .also { apiBrukerprofil ->
            appLogger.trace("Returnerer brukerprofil: tjenestestatus=${apiBrukerprofil?.tjenestestatus}, antallSøk=${apiBrukerprofil?.stillingssoek?.size}")
        }
}


suspend fun BrukerprofilTjeneste.setTjenestatestatus(
    identitetsnummer: Identitetsnummer,
    tjenesteStatus: ApiTjenesteStatus
): Response<Unit> {
    return suspendedTransactionAsync {
        hentLokalBrukerprofil(identitetsnummer)
            .coFlatMap { brukerProfil ->
                when (tjenesteStatus) {
                    ApiTjenesteStatus.AKTIV -> aktiverTjenesten(brukerProfil)
                    ApiTjenesteStatus.INAKTIV -> deaktiverTjenesten(brukerProfil)
                    ApiTjenesteStatus.OPT_OUT -> optOutAvTjenesten(brukerProfil)
                    ApiTjenesteStatus.KAN_IKKE_LEVERES -> oppdateringIkkeTillatt("Kan ikke sette tjenestestatus til KAN_IKKE_LEVERES manuelt.")
                }
            }
    }.await()
}

fun BrukerprofilTjeneste.optOutAvTjenesten(brukerProfil: BrukerProfil): Response<Unit> {
    val tidspunkt = clock.now()
    val gjeldeneFlagg = brukerProfil.listeMedFlagg
    val oppdaterteFlagg = gjeldeneFlagg.addOrUpdate(
        OptOutFlaggtype.flagg(true, tidspunkt),
        TjenestenErAktivFlaggtype.flagg(false, tidspunkt),
    )
    val oppdatering = OppdateringAvFlagg(
        nyeOgOppdaterteFlagg = oppdaterteFlagg.flaggSomMåOppdateres.toList(),
        søkSkalSlettes = true
    )
    oppdaterFlagg(brukerProfil.id, oppdatering)
    return Data(Unit)
}

fun BrukerprofilTjeneste.deaktiverTjenesten(brukerProfil: BrukerProfil): Response<Unit> {
    val tidspunkt = clock.now()
    val gjeldeneFlagg = brukerProfil.listeMedFlagg
    val oppdaterteFlagg = gjeldeneFlagg.addOrUpdate(
        TjenestenErAktivFlaggtype.flagg(false, tidspunkt),
    )
    val oppdatering = OppdateringAvFlagg(
        nyeOgOppdaterteFlagg = oppdaterteFlagg.flaggSomMåOppdateres.toList(),
        søkSkalSlettes = false
    )
    oppdaterFlagg(brukerProfil.id, oppdatering)
    return Data(Unit)
}

suspend fun BrukerprofilTjeneste.aktiverTjenesten(
    brukerProfil: BrukerProfil
): Response<Unit> {
    val tidspunkt = clock.now()
    return when (brukerProfil.tjenestenKanAktiveres(tidspunkt, ADRESSEBESKYTTELSE_GYLDIGHETS_PERIODE)) {
        TjenestenKanAktiveresResultat.Ja -> Data(brukerProfil)
        TjenestenKanAktiveresResultat.Nei -> oppdateringIkkeTillatt("Kan ikke aktivere tjenesten for denne brukeren.")
        TjenestenKanAktiveresResultat.AdressebeskyttelseMåSjekkes -> Data(
            oppdaterAdresseGradering(
                brukerProfil,
                tidspunkt
            )
        )
    }.flatMap { profil ->
        when (profil.tjenestenKanAktiveres(tidspunkt, ADRESSEBESKYTTELSE_GYLDIGHETS_PERIODE)) {
            TjenestenKanAktiveresResultat.Ja -> Data(profil)
            TjenestenKanAktiveresResultat.Nei -> oppdateringIkkeTillatt("Kan ikke aktivere tjenesten for denne brukeren.")
            TjenestenKanAktiveresResultat.AdressebeskyttelseMåSjekkes -> internFeil("Resultat='ADRESSE_GRADERING_MÅ_SJEKKES', selv rett etter oppdatering av adressegradering.")
        }
    }.map { profil ->
        val gjeldeneFlagg = profil.listeMedFlagg
        val oppdaterteFlagg = gjeldeneFlagg.addOrUpdate(
            HarBruktTjenestenFlaggtype.flagg(true, tidspunkt),
            TjenestenErAktivFlaggtype.flagg(true, tidspunkt),
            OptOutFlaggtype.flagg(false, tidspunkt),
        )
        val oppdatering = OppdateringAvFlagg(
            nyeOgOppdaterteFlagg = oppdaterteFlagg.flaggSomMåOppdateres.toList(),
            søkSkalSlettes = false
        )
        oppdaterFlagg(profil.id, oppdatering)
    }
}


suspend fun <A, B> Response<A>.coMap(transform: suspend (A) -> B): Response<B> {
    return when (this) {
        is Data -> Data(transform(this.data))
        is ProblemDetails -> this
    }
}

suspend fun <A, B> Response<A>.coFlatMap(transform: suspend (A) -> Response<B>): Response<B> {
    return when (this) {
        is Data -> transform(this.data)
        is ProblemDetails -> this
    }
}
