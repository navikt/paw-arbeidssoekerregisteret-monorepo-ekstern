package no.naw.paw.minestillinger.db.ops

import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.naw.paw.minestillinger.db.ProfileringTable
import no.naw.paw.minestillinger.domain.interntFormat
import org.jetbrains.exposed.v1.jdbc.upsert

fun lagreProfilering(profilering: Profilering) {
    ProfileringTable.upsert(
        keys = arrayOf(ProfileringTable.periodeId),
        onUpdateExclude = listOf(ProfileringTable.id, ProfileringTable.periodeId),
        body = {
            it[periodeId] = profilering.periodeId
            it[profileringId] = profilering.id
            it[profileringTidspunkt] = profilering.sendtInnAv.tidspunkt
            it[profileringResultat] = profilering.profilertTil.interntFormat().name
        }
    )
}