package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.aareg.model.*
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

private val sequence = AtomicLong(0)

fun arbeidsforhold(
    arbeidssted: Arbeidssted = arbeidssted(),
    fra: String,
    til: String? = null
) = Arbeidsforhold(
    arbeidssted = arbeidssted,
    ansettelsesperiode = ansettelsesPeriode(
        startDato = fra,
        sluttDato = til
    ),
    opplysningspliktig = Opplysningspliktig(
        type = "",
        identer = arbeidssted.identer
    ),
    ansettelsesdetaljer = listOf(
        Ansettelsesdetaljer(
            type = "Ordinaer",
            avtaltStillingsprosent = 100.0
        )
    ),
    opprettet = (til?.let(LocalDate::parse)?.atStartOfDay() ?: LocalDate.parse(fra).atStartOfDay())
        .plusDays(60)
)

fun arbeidssted(
    type: String = "Arbeidssted ${sequence.getAndIncrement()}",
    identer: String = sequence.getAndIncrement()
        .toString().padStart(9, '0')
) = Arbeidssted(
    type = type,
    identer = listOf(Ident(
        type = "ORGANISASJONSNUMMER",
        ident = identer
    ))
)

fun ansettelsesPeriode(
    startDato: String,
    sluttDato: String? = null
) = Ansettelsesperiode(
    LocalDate.parse(startDato),
    sluttDato?.let(LocalDate::parse)
)
