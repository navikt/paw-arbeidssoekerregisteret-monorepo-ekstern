package no.nav.paw.arbeidssoekerregisteret.api.oppslag.database

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.PeriodeOpplysningerRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.*

object PeriodeOpplysningerFunctions {

    fun findForPeriodeId(periodeId: UUID): List<PeriodeOpplysningerRow> =
        PeriodeOpplysningerTable.selectAll()
            .where { PeriodeOpplysningerTable.periodeId eq periodeId }
            .map { it.toPeriodeOpplysningerRow() }

    fun findAll(): List<PeriodeOpplysningerRow> =
        PeriodeOpplysningerTable.selectAll()
            .map { it.toPeriodeOpplysningerRow() }

}
