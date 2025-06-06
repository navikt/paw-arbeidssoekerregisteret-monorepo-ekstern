package no.nav.paw.test.data.bekreftelse

import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Stopp
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.paavegneav.v1.vo.Start
import java.time.Duration
import java.util.UUID


fun startPaaVegneAv(
    periodeId: UUID = UUID.randomUUID(),
    bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER,
    grace: Duration = Duration.ofDays(14),
    interval: Duration = Duration.ofDays(7)
): PaaVegneAv =
    PaaVegneAv.newBuilder()
        .setHandling(
            Start
                .newBuilder()
                .setGraceMS(grace.toMillis())
                .setIntervalMS(interval.toMillis())
                .build()
        )
        .setPeriodeId(periodeId)
        .setBekreftelsesloesning(bekreftelsesloesning)
        .build()

fun stoppPaaVegneAv(
    periodeId: UUID = UUID.randomUUID(),
    bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER,
    fristBrutt: Boolean = false
): PaaVegneAv =
    PaaVegneAv.newBuilder()
        .setHandling(Stopp(fristBrutt))
        .setBekreftelsesloesning(bekreftelsesloesning)
        .setPeriodeId(periodeId)
        .build()