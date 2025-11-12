package no.naw.paw.minestillinger.db.ops

import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.felles.model.asIdentitetsnummer
import no.naw.paw.minestillinger.db.BrukerTable
import no.naw.paw.minestillinger.domain.BrukerId
import no.naw.paw.minestillinger.domain.BrukerProfilerUtenFlagg
import no.naw.paw.minestillinger.domain.PeriodeId
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

fun hentBrukerProfilUtenFlagg(identitetsnummer: Identitetsnummer): BrukerProfilerUtenFlagg? =
    transaction {
        BrukerTable.selectAll().where {
            BrukerTable.identitetsnummer eq identitetsnummer.value
        }
            .map(::brukerprofilUtenFlagg)
            .firstOrNull()
    }

fun brukerprofilUtenFlagg(row: ResultRow): BrukerProfilerUtenFlagg {
    return BrukerProfilerUtenFlagg(
        id = BrukerId(row[BrukerTable.id]),
        identitetsnummer = row[BrukerTable.identitetsnummer].asIdentitetsnummer(),
        arbeidssoekerperiodeId = PeriodeId(row[BrukerTable.arbeidssoekerperiodeId]),
        arbeidssoekerperiodeAvsluttet = row[BrukerTable.arbeidssoekerperiodeAvsluttet]
    )
}

fun slettHvorPeriodeAvsluttetFør(tidspunkt: Instant): Int {
    return transaction {
        BrukerTable.deleteWhere {
            BrukerTable.arbeidssoekerperiodeAvsluttet.isNotNull() and
                    (BrukerTable.arbeidssoekerperiodeAvsluttet less tidspunkt)
        }
    }
}
