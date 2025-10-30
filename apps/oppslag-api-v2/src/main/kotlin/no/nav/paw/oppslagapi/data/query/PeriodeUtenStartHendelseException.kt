package no.nav.paw.oppslagapi.data.query

import java.time.Duration

class PeriodeUtenStartHendelseException(
    val hendelseType: String,
    val hendelseAlder: Duration
): IllegalStateException("Periode uten start hendelse, eldste hendelse er av type $hendelseType og er $hendelseAlder gammel")