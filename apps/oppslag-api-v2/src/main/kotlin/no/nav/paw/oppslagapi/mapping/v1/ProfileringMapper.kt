package no.nav.paw.oppslagapi.mapping.v1

import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.EgenvurderingResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringAggregertResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringsResultat
import no.nav.paw.oppslagapi.model.v2.Egenvurdering
import no.nav.paw.oppslagapi.model.v2.Profilering
import no.nav.paw.oppslagapi.model.v2.ProfilertTil
import java.util.*

fun Profilering.toV1Profilering(): ProfileringResponse {
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

fun Profilering.toV1ProfileringAggregert(egenvurdering: Egenvurdering?): ProfileringAggregertResponse {
    return ProfileringAggregertResponse(
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
        jobbetSammenhengendeSeksAvTolvSisteManeder = jobbetSammenhengendeSeksAvTolvSisteMnd,
        egenvurderinger = egenvurdering?.toV1Egenvurderinger(opplysningerOmArbeidssokerId)
    )
}

fun Egenvurdering.toV1Egenvurderinger(opplysningerOmArbeidssokerId: UUID): List<EgenvurderingResponse> {
    return listOf(
        EgenvurderingResponse(
            egenvurderingId = this.id,
            periodeId = this.periodeId,
            opplysningerOmArbeidssoekerId = opplysningerOmArbeidssokerId,
            profileringId = this.profileringId,
            sendtInnAv = this.sendtInnAv.v1Metadata(),
            profilertTil = when (this.profilertTil) {
                ProfilertTil.UKJENT_VERDI -> ProfileringsResultat.UKJENT_VERDI
                ProfilertTil.UDEFINERT -> ProfileringsResultat.UDEFINERT
                ProfilertTil.ANTATT_GODE_MULIGHETER -> ProfileringsResultat.ANTATT_GODE_MULIGHETER
                ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ProfileringsResultat.ANTATT_BEHOV_FOR_VEILEDNING
                ProfilertTil.OPPGITT_HINDRINGER -> ProfileringsResultat.OPPGITT_HINDRINGER
            },
            egenvurdering = when (this.egenvurdering) {
                ProfilertTil.UKJENT_VERDI -> ProfileringsResultat.UKJENT_VERDI
                ProfilertTil.UDEFINERT -> ProfileringsResultat.UDEFINERT
                ProfilertTil.ANTATT_GODE_MULIGHETER -> ProfileringsResultat.ANTATT_GODE_MULIGHETER
                ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ProfileringsResultat.ANTATT_BEHOV_FOR_VEILEDNING
                ProfilertTil.OPPGITT_HINDRINGER -> ProfileringsResultat.OPPGITT_HINDRINGER
            }
        )
    )
}