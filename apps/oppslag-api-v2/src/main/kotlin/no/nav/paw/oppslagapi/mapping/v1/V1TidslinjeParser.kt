package no.nav.paw.oppslagapi.mapping.v1

import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.BekreftelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.OpplysningerOmArbeidssoekerAggregertResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Tidslinje

fun Tidslinje.v1Periode(): ArbeidssoekerperiodeResponse {
    return ArbeidssoekerperiodeResponse(
        periodeId = periodeId,
        startet = hendelser
            .firstNotNullOf { hendelse -> hendelse.periodeStartetV1 }
            .v1Metadata(),
        avsluttet = hendelser
            .firstNotNullOfOrNull { hendelse -> hendelse.periodeAvsluttetV1 }
            ?.v1Metadata()
    )
}

fun Tidslinje.v1Opplysninger(): List<OpplysningerOmArbeidssoekerResponse> =
    hendelser
        .mapNotNull { it.opplysningerV4 }
        .map { it.toV1() }

fun Tidslinje.v1OpplysningerAggregert(): List<OpplysningerOmArbeidssoekerAggregertResponse> =
    hendelser
        .mapNotNull { it.opplysningerV4 }
        .map { opplysninger ->
            opplysninger.toV1Aggregert(
                profilering = hendelser
                    .mapNotNull { it.profileringV1 }
                    .firstOrNull { it.opplysningerOmArbeidssokerId == opplysninger.id }
            )
        }

fun Tidslinje.v1Profileringer(): List<ProfileringResponse> =
    hendelser
        .mapNotNull { it.profileringV1 }
        .map { it.toV1Profilering() }

fun Tidslinje.v1Bekreftelser(): List<BekreftelseResponse> =
    hendelser
        .mapNotNull { it.bekreftelseV1?.bekreftelse }
        .map { it.v1Bekreftelse() }
