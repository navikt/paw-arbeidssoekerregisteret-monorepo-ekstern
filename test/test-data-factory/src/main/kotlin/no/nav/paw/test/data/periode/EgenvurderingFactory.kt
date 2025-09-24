package no.nav.paw.test.data.periode

import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.api.v3.Egenvurdering
import java.util.*

fun createEgenvurdering(
    id: UUID = UUID.randomUUID(),
    periodeId: UUID = UUID.randomUUID(),
    profileringId: UUID = UUID.randomUUID(),
    sendtInnAv: Metadata = MetadataFactory.create().build(),
    profilertTil: ProfilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER,
    egenvurdering: ProfilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER,
) = Egenvurdering(
    id,
    periodeId,
    profileringId,
    sendtInnAv,
    profilertTil,
    egenvurdering
)

fun createEgenvurderingFor(
    profilering: Profilering,
    id: UUID = UUID.randomUUID(),
    sendtInnAv: Metadata = MetadataFactory.create().build(),
    profilertTil: ProfilertTil = profilering.profilertTil,
    egenvurdering: ProfilertTil = ProfilertTil.ANTATT_GODE_MULIGHETER,
) = Egenvurdering(
    id,
    profilering.periodeId,
    profilering.id,
    sendtInnAv,
    profilertTil,
    egenvurdering
)
