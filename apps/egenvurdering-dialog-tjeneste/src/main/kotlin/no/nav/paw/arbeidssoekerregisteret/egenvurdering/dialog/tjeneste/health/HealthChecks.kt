package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.health

import no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.utils.buildApplicationLogger
import no.nav.paw.arbeidssokerregisteret.api.v2.Egenvurdering
import no.nav.paw.kafka.consumer.CommittingKafkaConsumerWrapper
import javax.sql.DataSource
import kotlin.use

val logger = buildApplicationLogger

fun isDatabaseReady(dataSource: DataSource): Boolean = runCatching {
    dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT 1").execute()
    }
}.onFailure { error ->
    logger.error("Databasen er ikke klar enda", error)
}.getOrDefault(false)

fun isKafkaConsumerReady(consumerWrapper: CommittingKafkaConsumerWrapper<Long, Egenvurdering>): Boolean {
    return consumerWrapper.isRunning().also { isRunning ->
        if (!isRunning) {
            logger.error("Kafka-consumeren kjører ikke")
        }
    }
}
