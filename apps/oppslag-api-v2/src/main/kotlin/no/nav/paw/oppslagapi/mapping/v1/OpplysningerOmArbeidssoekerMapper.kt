package no.nav.paw.oppslagapi.mapping.v1

import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.AnnetResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.BeskrivelseMedDetaljerResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.HelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.JaNeiVetIkke
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.JobbSituasjonBeskrivelse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.OpplysningerOmArbeidssoekerAggregertResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.UtdanningResponse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Annet
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Helse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Profilering
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Utdanning

fun OpplysningerOmArbeidssoeker.toV1(): OpplysningerOmArbeidssoekerResponse =
    OpplysningerOmArbeidssoekerResponse(
        opplysningerOmArbeidssoekerId = this.id,
        periodeId = this.periodeId,
        sendtInnAv = this.sendtInnAv.v1Metadata(),
        jobbsituasjon = this.jobbsituasjon?.beskrivelser?.map { it.toV1() } ?: emptyList(),
        utdanning = this.utdanning?.toV1(),
        helse = this.helse?.toV1(),
        annet = this.annet?.toV1()
    )

fun OpplysningerOmArbeidssoeker.toV1Aggregert(profilering: Profilering?): OpplysningerOmArbeidssoekerAggregertResponse =
    OpplysningerOmArbeidssoekerAggregertResponse(
        opplysningerOmArbeidssoekerId = this.id,
        periodeId = this.periodeId,
        sendtInnAv = this.sendtInnAv.v1Metadata(),
        jobbsituasjon = this.jobbsituasjon?.beskrivelser?.map { it.toV1() } ?: emptyList(),
        utdanning = this.utdanning?.toV1(),
        helse = this.helse?.toV1(),
        annet = this.annet?.toV1(),
        profilering = profilering?.toV1ProfileringAggregert()
    )

fun BeskrivelseMedDetaljer.toV1(): BeskrivelseMedDetaljerResponse =
    BeskrivelseMedDetaljerResponse(
        beskrivelse = JobbSituasjonBeskrivelse.valueOf(this.beskrivelse.name),
        detaljer = this.detaljer
    )

fun Utdanning.toV1(): UtdanningResponse =
    UtdanningResponse(
        nus = this.nus,
        bestaatt = this.bestaatt?.toV1(),
        godkjent = this.godkjent?.toV1()
    )

fun Helse.toV1(): HelseResponse =
    HelseResponse(
        helsetilstandHindrerArbeid = this.helsetilstandHindrerArbeid?.toV1() ?: JaNeiVetIkke.VET_IKKE,
    )

fun Annet.toV1(): AnnetResponse =
    AnnetResponse(
        andreForholdHindrerArbeid = this.andreForholdHindrerArbeid?.toV1()
    )

fun no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke.toV1(): JaNeiVetIkke? {
    return when (this) {
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke.JA -> JaNeiVetIkke.JA
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke.NEI -> JaNeiVetIkke.NEI
        no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.JaNeiVetIkke.VET_IKKE -> JaNeiVetIkke.VET_IKKE
        else -> null
    }
}


