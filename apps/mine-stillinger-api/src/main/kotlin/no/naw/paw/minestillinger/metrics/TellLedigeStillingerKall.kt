package no.naw.paw.minestillinger.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.api.common.AttributeKey.longKey
import io.opentelemetry.api.trace.Span
import no.naw.paw.ledigestillinger.model.Tag
import no.naw.paw.minestillinger.api.ApiStedSoek
import no.naw.paw.minestillinger.api.MineStillingerResponse
import no.naw.paw.minestillinger.api.vo.ApiTag
import no.naw.paw.minestillinger.domain.HarSoekeord
import java.time.Duration
import java.time.Instant

fun tellLedigeStillingerKall(meterRegistry: MeterRegistry, tidspunkt: Instant, svar: MineStillingerResponse) {
    val antallStillinger = svar.resultat.size
    val dagerSidenSist = if (svar.sistKjoert != null) Duration.between(svar.sistKjoert, tidspunkt).toDays() else -1
    Span.current().setAttribute(longKey("antall_treff"), antallStillinger)
    Span.current().setAttribute(longKey("antall_soekeord"), ((svar.soek as? HarSoekeord)?.soekeord?.size ?: -1))
    Span.current().setAttribute(longKey("antall_styrk08"), ((svar.soek as? ApiStedSoek)?.styrk08?.size ?: -1))
    val antallDirMeldt = svar.resultat.filter { it.tags.contains(ApiTag.DIREKTEMELDT_V1) }.size
    Span.current().setAttribute(longKey("antall_direkte_meldte"), antallDirMeldt)
    meterRegistry.counter(
        "paw_mine_stillinger_ledige_stillinger",
        "antall_stillinger", if (antallStillinger < 14) antallStillinger.toString() else "14+",
        "dager_siden_sist_kjoert", if (dagerSidenSist < 7) dagerSidenSist.toString() else "7+",
        "antall_direkte_meldte", if (antallDirMeldt < 14) antallDirMeldt.toString() else "14+",
    ).increment()
}