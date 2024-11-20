package no.nav.paw.aareg.model

import java.time.LocalDate
import java.time.LocalDateTime

sealed class Result {
    data class Success(val arbeidsforhold: List<Arbeidsforhold>) : Result()
    data class Failure(val error: Error) : Result()
}

data class Error(
    val meldinger: List<String>
)

data class Arbeidsforhold(
    val arbeidssted: Arbeidssted,
    val ansettelsesdetaljer: List<Ansettelsesdetaljer>,
    val opplysningspliktig: Opplysningspliktig,
    val ansettelsesperiode: Ansettelsesperiode,
    val opprettet: LocalDateTime
)

data class Ansettelsesperiode(
    val startdato: LocalDate,
    val sluttdato: LocalDate? = null
)

data class Arbeidssted(
    val type: String, // Underenehet, Person
    val identer: List<Ident>
)

data class Opplysningspliktig(
    val type: String, // Hovedenhet, Person
    val identer: List<Ident>
)

data class Ident(
    val type: String, // AKTORID, FOLKEREGISTERIDENT, ORGANISASJONSNUMMER
    val ident: String,
)

data class Ansettelsesdetaljer(
    val type: String,
    val avtaltStillingsprosent: Double,
)
