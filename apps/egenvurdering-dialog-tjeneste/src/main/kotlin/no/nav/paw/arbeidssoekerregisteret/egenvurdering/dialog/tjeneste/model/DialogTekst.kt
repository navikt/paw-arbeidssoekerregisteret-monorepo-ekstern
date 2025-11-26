package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.model

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit.MINUTES
import java.util.*

internal object DialogTekst {
    const val OVERSKRIFT = "Egenvurdering"

    const val NAV_GODE_MULIGHETER =
        "Nav sin vurdering: Vi tror du har gode muligheter til å komme i jobb uten en veileder eller tiltak fra Nav."
    const val NAV_BEHOV_FOR_VEILEDNING =
        "Nav sin vurdering: Vi tror du vil trenge hjelp fra en veileder for å nå ditt mål om arbeid."

    const val BRUKER_TRENGER_IKKE_VEILEDNING = "Min vurdering: Jeg klarer meg uten veileder"
    const val BRUKER_TRENGER_VEILEDNING = "Min vurdering: Jeg trenger en veileder for å komme i arbeid"
    const val BRUKER_ØNSKER_HJELP = "Min vurdering: Ja, jeg ønsker hjelp"
    const val BRUKER_VIL_KLARE_SEG_SELV = "Min vurdering: Nei, jeg vil gjerne klare meg selv"

    const val FOOTER_PREFIX = "Dette er en automatisk generert melding basert på egenvurdering mottatt fra bruker:"

    fun footer(innsendt: Instant): String = "$FOOTER_PREFIX ${formaterDato(innsendt)}."
}

private fun formaterDato(instant: Instant): String = dateTimeFormatter.format(instant.atZone(OSLO).truncatedTo(MINUTES))

private val OSLO = ZoneId.of("Europe/Oslo")
private val NB_NO = Locale.of("nb", "NO")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy 'kl.' HH:mm", NB_NO)
