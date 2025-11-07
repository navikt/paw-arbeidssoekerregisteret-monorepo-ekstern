package no.naw.paw.minestillinger.validering

import no.naw.paw.minestillinger.api.vo.ApiFylke
import no.naw.paw.minestillinger.api.vo.ApiKommune
import no.naw.paw.minestillinger.kodeverk.SSBFylke
import no.naw.paw.minestillinger.kodeverk.SSBKodeverk
import no.naw.paw.minestillinger.kodeverk.SSBKommune

object FylkerOgKommuner {
    val fylker: Map<String, SSBFylke> = SSBKodeverk.fylker.associateBy { it.fylkesnummer }
    val kommuner: Map<String, SSBKommune> = SSBKodeverk.kommuner.associateBy { it.kommunenummer }

    fun erGyldigFylke(apiFylke: ApiFylke): Boolean {
        val ssbFylke = fylker[apiFylke.fylkesnummer] ?: return false
        return apiFylke.navn in ssbFylke.nameList
    }

    fun erGyldigKommune(apiKommune: ApiKommune): Boolean {
        val ssbKommune = kommuner[apiKommune.kommunenummer] ?: return false
        return apiKommune.navn in ssbKommune.nameList
    }
}