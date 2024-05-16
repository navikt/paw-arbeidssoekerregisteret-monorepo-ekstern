package no.nav.paw.meldeplikttjeneste.tilstand

import java.time.Duration

val registeretErAnsvarlig = Ansvarlig(
    namespace = "paw",
    id = "meldeplikttjeneste"
)

val registerRegler = Regler(
    interval = Duration.ofDays(14),
    gracePeriode = Duration.ofDays(7)
)