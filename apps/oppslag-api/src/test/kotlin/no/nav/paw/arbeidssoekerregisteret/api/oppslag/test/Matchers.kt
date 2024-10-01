package no.nav.paw.arbeidssoekerregisteret.api.oppslag.test

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.AnnetResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ArbeidssoekerperiodeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.BrukerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.HelseResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.MetadataResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.ProfileringResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.TidspunktFraKildeResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.UtdanningResponse
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker
import no.nav.paw.arbeidssokerregisteret.api.v1.Helse
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.api.v2.Annet
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.api.v4.Utdanning

infix fun TidspunktFraKildeResponse.shouldBeEqualTo(tidspunktFraKilde: TidspunktFraKilde): TidspunktFraKildeResponse {
    tidspunkt shouldBe tidspunktFraKilde.tidspunkt
    avviksType.name shouldBe tidspunktFraKilde.avviksType.name
    return this
}

infix fun BrukerResponse.shouldBeEqualTo(bruker: Bruker): BrukerResponse {
    id shouldBe bruker.id
    type.name shouldBe bruker.type.name
    return this
}

infix fun MetadataResponse.shouldBeEqualTo(metadata: Metadata): MetadataResponse {
    tidspunkt shouldBe metadata.tidspunkt
    utfoertAv shouldBeEqualTo metadata.utfoertAv
    kilde shouldBe metadata.kilde
    aarsak shouldBe metadata.aarsak
    tidspunktFraKilde?.shouldBeEqualTo(metadata.tidspunktFraKilde)
    return this
}

infix fun UtdanningResponse.shouldBeEqualTo(utdanning: Utdanning): UtdanningResponse {
    nus shouldBe utdanning.nus
    bestaatt?.name shouldBe utdanning.bestaatt?.name
    godkjent?.name shouldBe utdanning.godkjent?.name
    return this
}

infix fun HelseResponse.shouldBeEqualTo(helse: Helse): HelseResponse {
    helsetilstandHindrerArbeid.name shouldBe helse.helsetilstandHindrerArbeid.name
    return this
}

infix fun AnnetResponse.shouldBeEqualTo(annet: Annet): AnnetResponse {
    andreForholdHindrerArbeid?.name shouldBe annet.andreForholdHindrerArbeid?.name
    return this
}

infix fun OpplysningerOmArbeidssoekerResponse.shouldBeEqualTo(opplysninger: OpplysningerOmArbeidssoeker): OpplysningerOmArbeidssoekerResponse {
    opplysningerOmArbeidssoekerId shouldBe opplysninger.id
    periodeId shouldBe opplysninger.periodeId
    sendtInnAv shouldBeEqualTo opplysninger.sendtInnAv
    utdanning?.shouldBeEqualTo(opplysninger.utdanning)
    helse?.shouldBeEqualTo(opplysninger.helse)
    annet?.shouldBeEqualTo(opplysninger.annet)
    return this
}

infix fun ArbeidssoekerperiodeResponse.shouldBeEqualTo(periode: Periode): ArbeidssoekerperiodeResponse {
    periode shouldNotBe null
    periodeId shouldBe periode.id
    startet shouldBeEqualTo periode.startet
    avsluttet?.shouldBeEqualTo(periode.avsluttet)
    return this
}

infix fun ProfileringResponse.shouldBeEqualTo(profilering: Profilering): ProfileringResponse {
    profileringId shouldBe profilering.id
    periodeId shouldBe profilering.periodeId
    opplysningerOmArbeidssoekerId shouldBe profilering.opplysningerOmArbeidssokerId
    sendtInnAv shouldBeEqualTo profilering.sendtInnAv
    jobbetSammenhengendeSeksAvTolvSisteManeder shouldBe profilering.jobbetSammenhengendeSeksAvTolvSisteMnd
    alder shouldBe profilering.alder
    return this
}
