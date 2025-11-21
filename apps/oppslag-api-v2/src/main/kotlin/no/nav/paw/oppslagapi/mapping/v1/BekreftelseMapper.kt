package no.nav.paw.oppslagapi.mapping.v1

import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.BekreftelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.BekreftelseSvarResponse
import no.nav.paw.oppslagapi.model.v2.Bekreftelse
import no.nav.paw.oppslagapi.model.v2.Bekreftelsesloesning

fun Bekreftelse.v1Bekreftelse(): BekreftelseResponse {
    return BekreftelseResponse(
        periodeId = periodeId,
        bekreftelsesloesning = when(bekreftelsesloesning) {
            Bekreftelsesloesning.UKJENT_VERDI -> no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.Bekreftelsesloesning.UKJENT_VERDI
            Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET -> no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.Bekreftelsesloesning.ARBEIDSSOEKERREGISTERET
            Bekreftelsesloesning.DAGPENGER -> no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.Bekreftelsesloesning.DAGPENGER
            Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING -> no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.Bekreftelsesloesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING
        },
        svar = BekreftelseSvarResponse(
            sendtInnAv = svar.sendtInnAv.v1Metadata(),
            gjelderFra = svar.gjelderFra,
            gjelderTil = svar.gjelderTil,
            harJobbetIDennePerioden = svar.harJobbetIDennePerioden,
            vilFortsetteSomArbeidssoeker = svar.vilFortsetteSomArbeidssoeker
        )
    )
}