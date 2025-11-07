package no.naw.paw.minestillinger.validering

import no.nav.paw.error.model.Response
import no.nav.paw.error.model.map
import no.naw.paw.minestillinger.api.vo.ApiFylke
import no.naw.paw.minestillinger.ugyldigVerdiIRequest

fun validerFylkerOgKommuner(fylker: List<ApiFylke>): Response<Unit> {
    return fylker.fold(ok) { acc, apiFylke ->
        acc.map {
            if (FylkerOgKommuner.erGyldigFylke(apiFylke)) {
                apiFylke.kommuner.fold(ok) { kommuneAcc, apiKommune ->
                    kommuneAcc.map {
                        if (FylkerOgKommuner.erGyldigKommune(apiKommune)) it else ugyldigVerdiIRequest(
                            felt = "kommune",
                            verdi = apiKommune.navn
                        )
                    }
                }
            } else {
                ugyldigVerdiIRequest(felt = "fylke", verdi = apiFylke.navn)
            }
        }
    }
}