package no.naw.paw.minestillinger.db.ops

import no.naw.paw.minestillinger.db.ProfileringTable
import no.naw.paw.minestillinger.domain.PeriodeId
import no.naw.paw.minestillinger.domain.Profilering
import no.naw.paw.minestillinger.domain.ProfileringResultat
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

fun hentProfileringOrNull(periodeId: PeriodeId): Profilering? = transaction {
    ProfileringTable.selectAll()
        .where {
            ProfileringTable.periodeId eq periodeId.verdi
        }.map { row ->
            Profilering(
                id = row[ProfileringTable.id],
                profileringId = row[ProfileringTable.profileringId],
                periodeId = PeriodeId(row[ProfileringTable.periodeId]),
                profileringTidspunkt = row[ProfileringTable.profileringTidspunkt],
                profileringResultat = ProfileringResultat.valueOf(row[ProfileringTable.profileringResultat])
            )
        }.firstOrNull()
}
