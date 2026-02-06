package no.nav.paw.arbeidssoekerregisteret.topology.punctuator

import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.kafka.processor.Punctuation
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.KeyValueStore
import java.util.concurrent.atomic.AtomicLong

private val logger = buildApplicationLogger

fun buildDeprekeringPunctuator(
    applicationConfig: ApplicationConfig
): Punctuation<Long, Toggle> {
    val deprekeringConfig = applicationConfig.deprekering

    return Punctuation(
        interval = deprekeringConfig.punctuatorSchedule,
        type = PunctuationType.WALL_CLOCK_TIME,
    ) { timestamp, context ->

        if (deprekeringConfig.punctuatorActivePartition != context.taskId().partition()) {
            logger.info(
                "Skedulert jobb for opprydding i deprekeringer på partisjon {} er deaktivert, skal kun kjøre på partisjon {}",
                context.taskId().partition(),
                deprekeringConfig.punctuatorActivePartition
            )
        } else {
            logger.info(
                "Skedulert jobb for opprydding i deprekeringer på partisjon {} startet {}",
                context.taskId().partition(),
                timestamp
            )

            val totalCount = AtomicLong(0)

            try {
                val stateStore: KeyValueStore<String, String> = context.getStateStore(deprekeringConfig.stateStore)

                stateStore.all().asSequence().forEach { keyValue ->
                    stateStore.delete(keyValue.key)
                    totalCount.incrementAndGet()
                }

                logger.info(
                    "Skedulert jobb for opprydding i deprekeringer på partisjon {} fullførte {}, slettet {}",
                    context.taskId().partition(),
                    timestamp,
                    totalCount
                )
            } catch (e: Exception) {
                logger.error("Feil oppsto i skedulert jobb for opprydding i deprekeringer", e)
            }
        }
    }
}
