package no.naw.paw.minestillinger.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.trace.Span
import no.naw.paw.minestillinger.api.ApiStedSoek
import no.naw.paw.minestillinger.api.MineStillingerResponse
import no.naw.paw.minestillinger.domain.HarSoekeord
import java.time.Duration
import java.time.Instant

fun tellLedigeStillingerKall(meterRegistry: MeterRegistry, tidspunkt: Instant, svar: MineStillingerResponse) {
    val antallStillinger = svar.resultat.size
    val dagerSidenSist = if (svar.sistKjoert != null) Duration.between(tidspunkt, svar.sistKjoert).toDays() else -1
    Span.current().setAttribute(longKey("antall_treff"), antallStillinger)
    Span.current().setAttribute(longKey("antall_soekeord"), ((svar.soek as? HarSoekeord)?.soekeord?.size ?: -1))
    Span.current().setAttribute(longKey("antall_styrk08"), ((svar.soek as? ApiStedSoek)?.styrk08?.size ?: -1))
    meterRegistry.counter(
        "paw_mine_stillinger_ledige_stillinger",
        "antall_stillinger", if (antallStillinger < 5) antallStillinger.toString() else "5+",
        "dager_siden_sist_kjoert", if (dagerSidenSist < 7) dagerSidenSist.toString() else "7+",
    ).increment()
}