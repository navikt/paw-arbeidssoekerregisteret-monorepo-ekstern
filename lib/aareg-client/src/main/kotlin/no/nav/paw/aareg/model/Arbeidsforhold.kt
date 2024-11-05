package no.nav.paw.aareg.model

import java.time.LocalDate
import java.time.LocalDateTime

data class Arbeidsforhold(
    val arbeidsgiver: Arbeidsgiver,
    val opplysningspliktig: Opplysningspliktig,
    val arbeidsavtaler: List<Arbeidsavtale>,
    val ansettelsesperiode: Ansettelsesperiode,
    val registrert: LocalDateTime
)

data class Arbeidsavtale(
    val stillingsprosent: Double?,
    val gyldighetsperiode: Periode
)

data class Ansettelsesperiode(
    val periode: Periode
)

data class Arbeidsgiver(
    val type: String,
    val organisasjonsnummer: String?
)

data class Periode(
    val fom: LocalDate,
    val tom: LocalDate? = null
)

data class Opplysningspliktig(
    val type: String,
    val organisasjonsnummer: String?
)
