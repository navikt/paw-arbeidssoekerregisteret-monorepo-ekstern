package no.nav.paw.arbeidssokerregisteret.arena.adapter.utils

import no.nav.paw.arbeidssokerregisteret.arena.v4.ArenaArbeidssokerregisterTilstand

fun ArenaArbeidssokerregisterTilstand.info(): String =
    "periodeId=${periode.id}, opplysningsId=${opplysningerOmArbeidssoeker.id}, profilering=${profilering.id}," +
            " opplysninger.periodeId=${opplysningerOmArbeidssoeker.periodeId}, profilering.periodeId=${profilering.periodeId}," +
            "profilering.opplysningsId=${profilering.opplysningerOmArbeidssokerId}"