package no.nav.paw.arbeidssoekerregisteret.topology.punctuator

import no.nav.paw.arbeidssoekerregisteret.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.model.DeprekeringStatus
import no.nav.paw.arbeidssoekerregisteret.model.Toggle
import no.nav.paw.arbeidssoekerregisteret.model.ToggleAction
import no.nav.paw.arbeidssoekerregisteret.utils.IdentitetsnummerCsvReader
import no.nav.paw.kafka.processor.Punctuation
import no.nav.paw.kafkakeygenerator.model.KafkaKeysResponse
import no.nav.paw.logging.logger.buildApplicationLogger
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.state.KeyValueStore
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicLong
import kotlin.io.path.name

private val logger = buildApplicationLogger

fun buildDeprekeringPunctuator(
    applicationConfig: ApplicationConfig,
    kafkaKeysFunction: (ident: String) -> KafkaKeysResponse
): Punctuation<Long, Toggle> {
    val deprekeringConfig = applicationConfig.deprekering
    val microfrontendToggleConfig = applicationConfig.microfrontendToggle

    return Punctuation(
        interval = deprekeringConfig.punctuatorSchedule,
        type = PunctuationType.WALL_CLOCK_TIME,
    ) { timestamp, context ->

        if (deprekeringConfig.deaktivert) {
            logger.info(
                "Skedulert jobb for deprekering av behovsvurdering på partisjon {} er deaktivert",
                context.taskId().partition()
            )
        } else if (deprekeringConfig.punctuatorActivePartition != context.taskId().partition()) {
            logger.info(
                "Skedulert jobb for deprekering av behovsvurdering på partisjon {} er deaktivert, skal kun kjøre på partisjon {}",
                context.taskId().partition(),
                deprekeringConfig.punctuatorActivePartition
            )
        } else {
            logger.info(
                "Skedulert jobb for deprekering av behovsvurdering på partisjon {} startet {}",
                context.taskId().partition(),
                timestamp
            )

            val fileCount = AtomicLong(0)
            val disabledCount = AtomicLong(0)
            val totalCount = AtomicLong(0)

            try {
                val filePath = Paths.get(deprekeringConfig.csvFil)
                val stateStore: KeyValueStore<String, String> = context.getStateStore(deprekeringConfig.stateStore)

                val rows = IdentitetsnummerCsvReader.readValues(filePath).asSequence()
                rows.forEach { row ->
                    fileCount.incrementAndGet()
                    stateStore.putIfAbsent(row.identitetsnummer, DeprekeringStatus.ENABLED.name)
                }
                logger.info("Leste {} rader fra CSV-fil {} i mappe {}", fileCount.get(), filePath.name, filePath.parent)

                stateStore.all().asSequence().forEach { keyValue ->
                    val identitetsnummer = keyValue.key
                    val status = DeprekeringStatus.valueOf(keyValue.value)

                    totalCount.incrementAndGet()

                    if (status == DeprekeringStatus.ENABLED) {
                        logger.debug(
                            "Sender disable for {} for identitetsnummer",
                            microfrontendToggleConfig.aiaBehovsvurdering
                        )

                        val toggle = Toggle(
                            action = ToggleAction.DISABLE,
                            ident = identitetsnummer,
                            microfrontendId = microfrontendToggleConfig.aiaBehovsvurdering,
                            initiatedBy = "paw"
                        )
                        context.forward(toggle.asRecord(arbeidssoekerId = kafkaKeysFunction(identitetsnummer).id))

                        stateStore.put(identitetsnummer, DeprekeringStatus.DISABLED.name)

                        disabledCount.incrementAndGet()
                    } else {
                        logger.debug(
                            "{} allerede disabled for identitetsnummer",
                            microfrontendToggleConfig.aiaBehovsvurdering
                        )
                    }
                }

                logger.info(
                    "Skedulert jobb for deprekering av behovsvurdering på partisjon {} fullførte {}, deaktiverte {} av {}",
                    context.taskId().partition(),
                    timestamp,
                    disabledCount,
                    totalCount
                )
            } catch (e: Exception) {
                logger.error("Feil oppsto i skedulert jobb for deprekering av behovsvurdering", e)
            }
        }
    }
}
