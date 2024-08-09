package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.arbeidssokerregisteret.profilering.personinfo.PersonInfo
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

fun evaluerAlder(alder: Int?): Set<ProfileringsTagger> {
    return when (alder) {
        null -> setOf(ProfileringsTagger.UKJENT_ALDER)
        in (18..59) -> setOf(ProfileringsTagger.ALDER_INDIKERER_STANDARD_INNSATS)
        else -> emptySet()
    }
}
fun alderVedTidspunkt(tidspunkt: Instant, personInfo: PersonInfo): Int? {
    val utgangspunkt = tidspunkt.atZone(ZoneId.systemDefault()).toLocalDate()
    return personInfo.foedselsdato?.let { Period.between(it, utgangspunkt).years }
        ?: personInfo.foedselsAar?.let { aar -> Period.between(LocalDate.of(aar, 7, 1), utgangspunkt).years }
}