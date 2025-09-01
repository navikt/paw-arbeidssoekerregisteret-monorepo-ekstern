package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.MINUTES
import java.util.*

private val OSLO = ZoneId.of("Europe/Oslo")
private val NB_NO = Locale.of("nb", "NO")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy 'kl.' HH:mm", NB_NO)

internal fun formaterDato(instant: Instant): String =
    dateTimeFormatter.format(instant.atZone(OSLO).truncatedTo(MINUTES))
