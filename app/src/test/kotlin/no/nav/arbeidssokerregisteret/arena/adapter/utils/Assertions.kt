package no.nav.arbeidssokerregisteret.arena.adapter.utils

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker

fun assertApiOpplysningerMatchesArenaOpplysninger(api: OpplysningerOmArbeidssoeker, arena: no.nav.paw.arbeidssokerregisteret.arena.v4.OpplysningerOmArbeidssoeker) {
    arena.id shouldBe api.id
    arena.periodeId shouldBe api.periodeId
    arena.utdanning.nus shouldBe api.utdanning.nus
    arena.helse.helsetilstandHindrerArbeid.name shouldBe api.helse.helsetilstandHindrerArbeid.name
    assertApiMetadataMatchesArenaMetadata(api.sendtInnAv, arena.sendtInnAv)
}

fun assertApiPeriodeMatchesArenaPeriode(api: Periode, arena: no.nav.paw.arbeidssokerregisteret.arena.v1.Periode) {
    arena.id shouldBe api.id
    arena.identitetsnummer shouldBe api.identitetsnummer
    assertApiMetadataMatchesArenaMetadata(api.startet, arena.startet)
    assertApiMetadataMatchesArenaMetadata(api.avsluttet, arena.avsluttet)
}

fun assertApiProfileringMatchesArenaProfilering(api: Profilering, arena: no.nav.paw.arbeidssokerregisteret.arena.v1.Profilering) {
    arena.id shouldBe api.id
    arena.opplysningerOmArbeidssokerId shouldBe api.opplysningerOmArbeidssokerId
    arena.periodeId shouldBe api.periodeId
    arena.profilertTil.name shouldBe api.profilertTil.name
    arena.alder shouldBe api.alder
    assertApiMetadataMatchesArenaMetadata(api.sendtInnAv, arena.sendtInnAv)
    arena.jobbetSammenhengendeSeksAvTolvSisteMnd shouldBe api.jobbetSammenhengendeSeksAvTolvSisteMnd
}

fun assertApiMetadataMatchesArenaMetadata(api: Metadata?, arena: no.nav.paw.arbeidssokerregisteret.arena.v1.Metadata?) {
    if (api == null) {
        arena shouldBe null
        return
    } else {
        arena.shouldNotBeNull()
        arena.tidspunkt shouldBe api.tidspunkt
        arena.aarsak shouldBe api.aarsak
        arena.kilde shouldBe api.kilde
        arena.utfoertAv.id shouldBe api.utfoertAv.id
        arena.utfoertAv.type.name shouldBe api.utfoertAv.type.name
    }
}