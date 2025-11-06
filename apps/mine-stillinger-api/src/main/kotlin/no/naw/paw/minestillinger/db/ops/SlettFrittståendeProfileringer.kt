package no.naw.paw.minestillinger.db.ops

import no.naw.paw.minestillinger.appLogger
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.db.ProfileringTable
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

fun slettFrittståendeProfileringer(
    profilertFør: Instant
): Int {
    return transaction {
        appLogger.info("Sletter profileringer uten tilknyttet arbeidssøkerperiode eldre enn $profilertFør")
        val skalSlettes = ProfileringTable
            .join(
                otherTable = BrukerTable,
                onColumn = ProfileringTable.periodeId,
                otherColumn = BrukerTable.arbeidssoekerperiodeId,
                joinType = JoinType.LEFT
            ).select(ProfileringTable.periodeId)
            .where { BrukerTable.arbeidssoekerperiodeId.isNull() and (ProfileringTable.profileringTidspunkt less profilertFør) }
            .map { it[BrukerTable.arbeidssoekerperiodeId] }
        val slettet = ProfileringTable.deleteWhere {
            ProfileringTable.periodeId.inList(skalSlettes)
        }
        appLogger.info("Slettet $slettet/${skalSlettes.size} frittstående profileringer")
        slettet
    }
}
