package no.nav.paw.arbeidssoekerregisteret.api.oppslag.consumer

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.ApplicationConfig
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.context.ApplicationContext
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

private val logger = LoggerFactory.getLogger("KafkaConsumerThreadPoolExecutor")

fun kafkaConsumerThreadPoolExecutor(applicationContext: ApplicationContext, config: ApplicationConfig) {
    val threadPoolExecutor = ThreadPoolExecutor(4, 8, 1, TimeUnit.MINUTES, LinkedBlockingQueue())
    runAsync({
        try {
            applicationContext.periodeKafkaConsumer.subscribe()
            applicationContext.opplysningerKafkaConsumer.subscribe()
            applicationContext.profileringKafkaConsumer.subscribe()
            applicationContext.bekreftelseKafkaConsumer.subscribe()

            while (true) {
                consume(applicationContext, config)
            }
        } catch (e: Exception) {
            logger.error("Consumer error: ${e.message}", e)
            exitProcess(1)
        }
    }, threadPoolExecutor)
}

@WithSpan(
    value = "consume",
    kind = SpanKind.INTERNAL
)
fun consume(
    applicationContext: ApplicationContext,
    config: ApplicationConfig
) {
    applicationContext.periodeKafkaConsumer.getAndProcessBatch(config.perioderTopic)
    applicationContext.opplysningerKafkaConsumer.getAndProcessBatch(config.opplysningerTopic)
    applicationContext.profileringKafkaConsumer.getAndProcessBatch(config.profileringTopic)
    applicationContext.bekreftelseKafkaConsumer.getAndProcessBatch(config.bekreftelseTopic)
}