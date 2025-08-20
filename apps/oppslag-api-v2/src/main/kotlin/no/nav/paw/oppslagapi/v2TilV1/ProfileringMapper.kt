package no.nav.paw.oppslagapi.v2TilV1

import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringsResultat
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Profilering
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.ProfilertTil

fun Profilering.v1Profilering(): ProfileringResponse {
    return ProfileringResponse(
        profileringId = id,
        periodeId = periodeId,
        opplysningerOmArbeidssoekerId = opplysningerOmArbeidssokerId,
        sendtInnAv = sendtInnAv.v1Metadata(),
        profilertTil = when (profilertTil) {
            ProfilertTil.UKJENT_VERDI -> ProfileringsResultat.UKJENT_VERDI
            ProfilertTil.UDEFINERT -> ProfileringsResultat.UDEFINERT
            ProfilertTil.ANTATT_GODE_MULIGHETER -> ProfileringsResultat.ANTATT_GODE_MULIGHETER
            ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ProfileringsResultat.ANTATT_BEHOV_FOR_VEILEDNING
            ProfilertTil.OPPGITT_HINDRINGER -> ProfileringsResultat.OPPGITT_HINDRINGER
        },
        alder = alder,
        jobbetSammenhengendeSeksAvTolvSisteManeder = jobbetSammenhengendeSeksAvTolvSisteMnd
    )
}
