package no.nav.paw.arbeidssokerregisteret.profilering.application.profilering

import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v1.JaNeiVetIkke.NEI
import no.nav.paw.arbeidssokerregisteret.profilering.application.profilering.ProfileringsTagger.ANDRE_FORHOLD_HINDRER_ARBEID

fun evaluerAnnet(annet: Annet?): Set<ProfileringsTagger> {
    return when {
        annet == null -> emptySet()
        annet.andreForholdHindrerArbeid == NEI -> emptySet()
        else -> setOf(ANDRE_FORHOLD_HINDRER_ARBEID)
    }
}