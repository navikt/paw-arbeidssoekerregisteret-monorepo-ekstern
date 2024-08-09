package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.aareg.Arbeidsforhold
import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo
import java.time.LocalDate

fun personInfo(
    vararg arbeidsforhold: Arbeidsforhold,
    foedselsdato: String?,
    foedselsAar: Int = requireNotNull(foedselsdato).substring(0, 4).toInt()
) = PersonInfo(
    foedselsdato = foedselsdato?.let(LocalDate::parse),
    foedselsAar = foedselsAar,
    arbeidsforhold = arbeidsforhold.toList()
)
