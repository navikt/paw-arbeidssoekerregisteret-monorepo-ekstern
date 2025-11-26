package no.nav.paw.arbeidssoekerregisteret.test

import no.nav.paw.felles.model.Identitetsnummer
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssokerregisteret.api.v1.Bruker as AvroBruker
import no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType as AvroBrukerType
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as AvroMetadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode as AvroPeriode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering as AvroProfilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil as AvroProfilertTil

object TestData {

    fun periode(
        id: UUID = UUID.randomUUID(),
        identitetsnummer: Identitetsnummer = Identitetsnummer("01017012345"),
        startet: AvroMetadata = metadata(identitetsnummer),
        avsluttet: AvroMetadata? = null
    ): AvroPeriode = AvroPeriode(
        id,
        identitetsnummer.value,
        startet,
        avsluttet
    )

    fun profilering(
        id: UUID = UUID.randomUUID(),
        periodeId: UUID = UUID.randomUUID(),
        opplysningerId: UUID = UUID.randomUUID(),
        sendtInnAv: AvroMetadata = metadata(),
        profilertTil: AvroProfilertTil = AvroProfilertTil.ANTATT_GODE_MULIGHETER
    ): AvroProfilering = AvroProfilering(
        id,
        periodeId,
        opplysningerId,
        sendtInnAv,
        profilertTil,
        false,
        42
    )

    fun metadata(
        identitetsnummer: Identitetsnummer = Identitetsnummer("01017012345"),
        bruker: AvroBruker = bruker(identitetsnummer),
        tidspunkt: Instant = Instant.now()
    ): AvroMetadata = AvroMetadata(
        tidspunkt,
        bruker,
        "test",
        "test",
        null
    )

    fun bruker(
        identitetsnummer: Identitetsnummer
    ): AvroBruker = AvroBruker(
        AvroBrukerType.SLUTTBRUKER,
        identitetsnummer.value,
        "tokenx:Level4"
    )
}