package no.nav.paw.test.data.periode

import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import java.util.UUID

fun createProfilering(
    id: UUID = UUID.randomUUID(),
    periodeId: UUID = UUID.randomUUID(),
    opplysningerId: UUID = UUID.randomUUID(),
    sendtInnAv: Metadata = MetadataFactory.create().build(),
    profilertTil: ProfilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER,
    jobbetSammenhengende: Boolean = true,
    alder: Int = 42
): Profilering =
    Profilering(
        id,
        periodeId,
        opplysningerId,
        sendtInnAv,
        profilertTil,
        jobbetSammenhengende,
        alder
    )