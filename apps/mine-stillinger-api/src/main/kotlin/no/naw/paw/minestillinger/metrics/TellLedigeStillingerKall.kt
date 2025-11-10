package no.naw.paw.minestillinger.metrics

import io.micrometer.core.instrument.MeterRegistry
import no.naw.paw.minestillinger.api.MineStillingerResponse
import java.time.Duration
import java.time.Instant

fun tellLedigeStillingerKall(meterRegistry: MeterRegistry, tidspunkt: Instant, svar: MineStillingerResponse) {
    val antallStillinger = svar.resultat.size
    val dagerSidenSist = if (svar.sistKjoert != null) Duration.between(tidspunkt, svar.sistKjoert).toDays() else -1
    meterRegistry.counter(
        "paw_mine_stillinger_ledige_stillinger",
        "antall_stillinger", if (antallStillinger < 5) antallStillinger.toString() else "5+",
        "dager_siden_sist_kjoert", if (dagerSidenSist < 7) dagerSidenSist.toString() else "7+",
    ).increment()
}