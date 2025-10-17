package no.nav.paw.ledigestillinger.service

import no.nav.pam.stilling.ext.avro.Ad
import no.nav.paw.hwm.Message
import no.nav.paw.ledigestillinger.model.asStillingRow
import no.nav.paw.ledigestillinger.model.dao.ArbeidsgivereTable
import no.nav.paw.ledigestillinger.model.dao.BeliggenheterTable
import no.nav.paw.ledigestillinger.model.dao.EgenskaperTable
import no.nav.paw.ledigestillinger.model.dao.KategorierTable
import no.nav.paw.ledigestillinger.model.dao.KlassifiseringerTable
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.model.dao.insert
import no.nav.paw.ledigestillinger.model.dao.selectIdByUUID
import no.nav.paw.logging.logger.buildLogger
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.*

class StillingService {
    private val logger = buildLogger

    fun handleMessages(messages: Sequence<Message<UUID, Ad>>) = transaction {
        messages
            .onEach { message -> logger.debug("Mottatt melding på topic=${message.topic}, partition=${message.partition}, offset=${message.offset}") }
            .forEach { message ->
                runCatching {
                    val stillingRow = message.asStillingRow()
                    val existingId = StillingerTable.selectIdByUUID(message.key)
                    if (existingId == null) {
                        val id = StillingerTable.insert(stillingRow)
                        stillingRow.arbeidsgiver?.let { row ->
                            ArbeidsgivereTable.insert(
                                parentId = id,
                                row = row
                            )
                        }
                        stillingRow.kategorier.forEach { row ->
                            KategorierTable.insert(
                                parentId = id,
                                row = row
                            )
                        }
                        stillingRow.klassifiseringer.forEach { row ->
                            KlassifiseringerTable.insert(
                                parentId = id,
                                row = row
                            )
                        }
                        stillingRow.beliggenheter.forEach { row ->
                            BeliggenheterTable.insert(
                                parentId = id,
                                row = row
                            )
                        }
                        stillingRow.egenskaper.forEach { row ->
                            EgenskaperTable.insert(
                                parentId = id,
                                row = row
                            )
                        }
                    } else {
                        logger.warn("Stilling med samme UUID er allerede mottatt, ignorerer melding mens vi tester!")
                    }
                }.onFailure { cause ->
                    logger.error("Feil ved mottak av melding", cause)
                }.getOrThrow()
            }
    }
}