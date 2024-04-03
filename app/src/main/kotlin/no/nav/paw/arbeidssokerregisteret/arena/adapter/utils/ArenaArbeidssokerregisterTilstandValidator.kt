package no.nav.paw.arbeidssokerregisteret.arena.adapter.utils

import no.nav.paw.arbeidssokerregisteret.arena.v5.ArenaArbeidssokerregisterTilstand

val ArenaArbeidssokerregisterTilstand.isValid: Boolean
    get() {
        val periodeId = periode.id
        val opplysningerPeriodeId = opplysningerOmArbeidssoeker.periodeId
        val profileringPeriodeId = profilering.periodeId
        val profileringOpplysningsId = profilering.opplysningerOmArbeidssokerId
        val validMatch = periodeId != opplysningerPeriodeId || periodeId != profileringPeriodeId ||
                profileringOpplysningsId != opplysningerOmArbeidssoeker.id
        return validMatch
    }