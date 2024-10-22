package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.database.OpplysningerFunctions
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class OpplysningerRepository(private val database: Database) {

    private val logger = buildLogger

    fun finnOpplysningerForPeriodeId(periodeId: UUID): List<OpplysningerRow> =
        transaction(database) {
            OpplysningerFunctions.findForPeriodeId(periodeId)
        }

    fun finnOpplysningerForIdentiteter(identitetsnummerList: List<Identitetsnummer>): List<OpplysningerRow> =
        transaction(database) {
            // TODO Optimalisering vha joins
            val periodeIder = PeriodeRepository(database).finnPerioderForIdentiteter(identitetsnummerList)
                .map { it.periodeId }
            periodeIder.flatMap { periodeId ->
                OpplysningerFunctions.findForPeriodeId(periodeId)
            }
        }

    fun lagreOpplysninger(opplysninger: OpplysningerOmArbeidssoeker) {
        transaction(database) {
            val eksisterendeOpplysninger = OpplysningerFunctions.getRow(opplysninger.id)

            if (eksisterendeOpplysninger != null) {
                logger.warn("Opplysning med samme ID finnes allerede i databasen, ignorer derfor ny opplysning som duplikat")
            } else {
                OpplysningerFunctions.insert(opplysninger)
            }
        }
    }

    fun lagreAlleOpplysninger(opplysninger: Sequence<OpplysningerOmArbeidssoeker>) {
        if (opplysninger.iterator().hasNext()) {
            transaction(database) {
                maxAttempts = 2
                minRetryDelay = 20

                val opplysningerIdList = opplysninger.map { it.id }.toList()
                val eksisterendeOpplysningerList = OpplysningerFunctions.finnRows(opplysningerIdList)
                val eksisterendeOpplysningerMap = eksisterendeOpplysningerList.associateBy { it.opplysningerId }

                opplysninger.forEach { opplysninger ->
                    val eksisterendeOpplysninger = eksisterendeOpplysningerMap[opplysninger.id]
                    if (eksisterendeOpplysninger != null) {
                        logger.warn("Ignorerer mottatte opplysninger {} som duplikat", opplysninger.id)
                    } else {
                        logger.debug("Lagrer nye opplysninger {}", opplysninger.id)
                        OpplysningerFunctions.insert(opplysninger)
                    }
                }
            }
        }
    }
}
