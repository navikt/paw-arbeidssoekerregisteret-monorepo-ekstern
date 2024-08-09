package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke.NEI
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.ProfileringsTagger.HELSETILSTAND_HINDRER_ARBEID

fun evaluerHelse(helse: Helse?): Set<ProfileringsTagger> {
    return when {
        helse == null -> emptySet()
        helse.helsetilstandHindrerArbeid == NEI -> emptySet()
        else -> setOf(HELSETILSTAND_HINDRER_ARBEID)
    }
}