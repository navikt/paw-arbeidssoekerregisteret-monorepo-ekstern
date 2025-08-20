package no.nav.paw.oppslagapi.v2TilV1

import no.nav.paw.arbeidssoekerregisteret.api.v1.oppslag.models.*
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Annet
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.AvviksType
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.BeskrivelseMedDetaljer
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Bruker
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Helse
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.Metadata
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssoekerregisteret.api.v2.oppslag.models.TidspunktFraKilde
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

fun BeskrivelseMedDetaljer.toV1(): BeskrivelseMedDetaljerResponse =
    BeskrivelseMedDetaljerResponse(
        beskrivelse = JobbSituasjonBeskrivelse.valueOf(this.beskrivelse.name),
        detaljer = this.detaljer
    )

fun Utdanning.toV1(): UtdanningResponse =
    UtdanningResponse(
        nus = this.nus,
        bestaatt = this.bestaatt?.let { JaNeiVetIkke.valueOf(it.name) },
        godkjent = this.godkjent?.let { JaNeiVetIkke.valueOf(it.name) }
    )

fun Helse.toV1(): HelseResponse =
    HelseResponse(
        helsetilstandHindrerArbeid = this.helsetilstandHindrerArbeid?.let { JaNeiVetIkke.valueOf(it.name) }
            ?: JaNeiVetIkke.VET_IKKE,
    )

fun Annet.toV1(): AnnetResponse =
    AnnetResponse(
        andreForholdHindrerArbeid = this.andreForholdHindrerArbeid?.let { JaNeiVetIkke.valueOf(it.name) }
    )

