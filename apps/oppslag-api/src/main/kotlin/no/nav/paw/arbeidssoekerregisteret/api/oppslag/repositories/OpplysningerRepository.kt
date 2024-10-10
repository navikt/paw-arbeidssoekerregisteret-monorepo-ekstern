package no.nav.paw.arbeidssoekerregisteret.api.oppslag.repositories

import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.Identitetsnummer
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.models.OpplysningerOmArbeidssoekerResponse
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.buildLogger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.finnOpplysninger
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.finnOpplysningerRow
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.finnOpplysningerRows
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils.opprettOpplysninger
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class OpplysningerRepository(private val database: Database) {

    private val logger = buildLogger

    fun finnOpplysningerForPeriodeId(periodeId: UUID): List<OpplysningerOmArbeidssoekerResponse> =
        transaction(database) {
            finnOpplysninger(periodeId)
        }

    fun finnOpplysningerForIdentiteter(identitetsnummerList: List<Identitetsnummer>): List<OpplysningerOmArbeidssoekerResponse> =
        transaction(database) {
            // TODO Optimalisering vha joins
            val periodeIder = PeriodeRepository(database).finnPerioderForIdentiteter(identitetsnummerList)
                .map { it.periodeId }
            periodeIder.flatMap { periodeId ->
                finnOpplysninger(periodeId)
            }
        }

    fun lagreOpplysninger(opplysninger: OpplysningerOmArbeidssoeker) {
        transaction(database) {
            val eksisterendeOpplysninger = finnOpplysningerRow(opplysninger.id)

            if (eksisterendeOpplysninger != null) {
                logger.warn("Opplysning med samme ID finnes allerede i databasen, ignorer derfor ny opplysning som duplikat")
            } else {
                opprettOpplysninger(opplysninger)
            }
        }
    }

    fun lagreAlleOpplysninger(opplysninger: Sequence<OpplysningerOmArbeidssoeker>) {
        if (opplysninger.iterator().hasNext()) {
            transaction(database) {
                maxAttempts = 2
                minRetryDelay = 20

                val opplysningerIdList = opplysninger.map { it.id }.toList()
                val eksisterendeOpplysningerList = finnOpplysningerRows(opplysningerIdList)
                val eksisterendeOpplysningerMap = eksisterendeOpplysningerList.associateBy { it.opplysningerId }

                opplysninger.forEach { opplysninger ->
                    val eksisterendeOpplysninger = eksisterendeOpplysningerMap[opplysninger.id]
                    if (eksisterendeOpplysninger != null) {
                        logger.warn("Opplysning med samme ID finnes allerede i databasen, ignorer derfor ny opplysning som duplikat")
                    } else {
                        opprettOpplysninger(opplysninger)
                    }
                }
            }
        }
    }
}
