package no.naw.paw.minestillinger.metrics

import io.micrometer.core.instrument.MeterRegistry
import no.naw.paw.minestillinger.api.vo.ApiBrukerprofil

fun tellHentBrukerpofilKall(meterRegistry: MeterRegistry, brukerprofil: ApiBrukerprofil) {
    meterRegistry.counter(
        "paw_mine_stillinger_brukerprofil",
        "tjenestestatus", brukerprofil.tjenestestatus.name.lowercase(),
        "har_soek", brukerprofil.stillingssoek.isNotEmpty().toString().lowercase(),
    ).increment()
}