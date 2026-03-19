package no.naw.paw.minestillinger.metrics

import io.micrometer.core.instrument.MeterRegistry
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil
import no.naw.paw.minestillinger.api.vo.ApiFlagg
import no.naw.paw.minestillinger.api.vo.ApiFlaggNavn
import no.naw.paw.minestillinger.brukerprofil.flagg.InkluderDirekteMeldteStillingerFlagtype

fun tellHentBrukerpofilKall(meterRegistry: MeterRegistry, brukerprofil: ApiBrukerprofil) {
    meterRegistry.counter(
        "paw_mine_stillinger_brukerprofil",
        "tjenestestatus", brukerprofil.tjenestestatus.name.lowercase(),
        "har_soek", brukerprofil.stillingssoek.isNotEmpty().toString().lowercase(),
        "inkluder_direktemeldte_stillinger", brukerprofil.flagg.any { it.navn == ApiFlaggNavn.DIREKTEMELDTE_STILLINGER }.toString()
    ).increment()
}