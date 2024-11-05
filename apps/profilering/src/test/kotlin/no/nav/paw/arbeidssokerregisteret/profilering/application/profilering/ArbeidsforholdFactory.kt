package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.aareg.model.Ansettelsesperiode
import no.nav.paw.aareg.model.Arbeidsforhold
import no.nav.paw.aareg.model.Arbeidsgiver
import no.nav.paw.aareg.model.Opplysningspliktig
import no.nav.paw.aareg.model.Periode
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicLong

private val sequence = AtomicLong(0)

fun arbeidsforhold(
    arbeidsgiver: Arbeidsgiver = arbeidsgiver(),
    fra: String,
    til: String? = null
) = Arbeidsforhold(
    arbeidsgiver = arbeidsgiver,
    ansettelsesperiode = ansettelsesPeriode(
        fra = fra,
        til = til
    ),
    opplysningspliktig = Opplysningspliktig(
        type = "",
        organisasjonsnummer = arbeidsgiver.organisasjonsnummer
    ),
    arbeidsavtaler = emptyList(),
    registrert = (til?.let(LocalDate::parse)?.atStartOfDay() ?: LocalDate.parse(fra).atStartOfDay())
        .plusDays(60)
)

fun arbeidsgiver(
    type: String = "Arbeidsgiver ${sequence.getAndIncrement()}",
    organisasjonsnummer: String = sequence.getAndIncrement()
        .toString().padStart(9, '0')
) = Arbeidsgiver(
    type = type,
    organisasjonsnummer = organisasjonsnummer
)

fun ansettelsesPeriode(
    fra: String,
    til: String? = null
) = Ansettelsesperiode(
    periode = Periode(
        fom = LocalDate.parse(fra),
        tom = til?.let(LocalDate::parse)
    )
)
