package no.naw.paw.minestillinger

import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.client.factory.createHttpClient
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.felles.model.AktorId
import no.nav.paw.felles.model.Identitetsnummer
import no.nav.paw.health.healthChecksOf
import no.nav.paw.health.probes.DatasourceLivenessProbe
import no.nav.paw.hwm.HwmTopicConfig
import no.nav.paw.hwm.Message
import no.nav.paw.hwm.asMessageConsumerWithHwmAndMetrics
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.model.IdentitetType
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.texas.TEXAS_CONFIG
import no.nav.paw.security.texas.TexasClient
import no.nav.paw.security.texas.TexasClientConfig
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste
import no.naw.paw.minestillinger.db.initDatabase
import no.naw.paw.minestillinger.db.ops.hentBrukerProfilUtenFlagg
import no.naw.paw.minestillinger.db.ops.hentProfileringOrNull
import no.naw.paw.minestillinger.db.ops.lagreProfilering
import no.naw.paw.minestillinger.db.ops.lesFlaggFraDB
import no.naw.paw.minestillinger.db.ops.opprettOgOppdaterBruker
import no.naw.paw.minestillinger.db.ops.skrivFlaggTilDB
import no.naw.paw.minestillinger.db.ops.slettAlleSoekForBruker
import no.naw.paw.minestillinger.vedtak14a.Siste14aVedtakMelding
import no.naw.paw.minestillinger.vedtak14a.lagre14aResultat
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.LongDeserializer
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory
import java.util.*

val appLogger = LoggerFactory.getLogger("brukerprofiler.api")
val httpLogger = LoggerFactory.getLogger("brukerprofiler.api.http")

fun main() {
    appLogger.info("Starter brukerprofiler-api...")
    val clock = SystemClock
    val topicNames = standardTopicNames(currentRuntimeEnvironment)
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val securityConfig: SecurityConfig = loadNaisOrLocalConfiguration(SECURITY_CONFIG)
    val topics = listOf(
        HwmTopicConfig(
            topic = topicNames.periodeTopic,
            consumerVersion = PERIODE_CONSUMER_VERSION
        ),
        HwmTopicConfig(
            topic = topicNames.profileringTopic,
            consumerVersion = PROFILERING_CONSUMER_VERSION
        ),
        HwmTopicConfig(
            topic = SISTE_14A_VEDTAK_TOPIC,
            consumerVersion = SISTE_14A_VEDTAK_CONSUMER_VERSION
        )
    )
    val kafkaFactory = KafkaFactory(loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG))
    val topicAwareValueDeserializer = kafkaFactory.initTopicAwareValueDeserializer(
        topicNames = topicNames,
        siste14aVedtakTopic = SISTE_14A_VEDTAK_TOPIC
    )
    val topicAwareKeyDeserializer = initTopicAwareKeyDeserializer(
        topicNames = topicNames,
        siste14aVedtakTopic = SISTE_14A_VEDTAK_TOPIC
    )
    val dataSource = initDatabase(loadNaisOrLocalConfiguration(DATABASE_CONFIG))
    val webClients = initWebClient()
    val brukerprofilTjeneste = BrukerprofilTjeneste(
        meterRegistry = prometheusMeterRegistry,
        pdlClient = webClients.pdlClient,
        hentBrukerprofilUtenFlagg = ::hentBrukerProfilUtenFlagg,
        skrivFlagg = ::skrivFlaggTilDB,
        hentFlagg = ::lesFlaggFraDB,
        hentProfilering = ::hentProfileringOrNull,
        slettAlleSøk = ::slettAlleSoekForBruker,
        abTestingRegex = requireNotNull(System.getenv("AB_TESTING_REGEX")?.toRegex()) { "AB_TESTING_REGEX env variabel må være satt" },
        clock = clock
    )
    val processorContext = ProcessorContext(
        brukerprofilTjeneste = brukerprofilTjeneste,
        idFunction = { aktorId ->
            runBlocking {
                webClients.kafkaKeysClient.getIdentiteter(
                    identitetsnummer = aktorId.value,
                    visKonflikter = false,
                    hentFraPdl = false
                )
            }.identiteter
                .firstOrNull { it.gjeldende && it.type == IdentitetType.FOLKEREGISTERIDENT }
                ?.identitet
                ?.let(::Identitetsnummer)
        }

    )
    val consumer = kafkaFactory.createConsumer(
        groupId = "brukerprofiler_api_consumer_v1",
        clientId = "brukerprofiler_api_v1_${UUID.randomUUID()}",
        keyDeserializer = topicAwareKeyDeserializer,
        valueDeserializer = topicAwareValueDeserializer
    ).asMessageConsumerWithHwmAndMetrics(
        prometheusMeterRegistry = prometheusMeterRegistry,
        receiver = processorContext::process,
        hwmTopicConfig = topics
    )

    val bakgrunnsprosesser = initBakgrunnsprosesser(
        webClients = webClients,
        clock = clock,
        brukerprofilTjeneste = brukerprofilTjeneste,
        prometheusMeterRegistry = prometheusMeterRegistry
    )
    val texasConfig: TexasClientConfig = loadNaisOrLocalConfiguration(TEXAS_CONFIG)
    val texasClient = TexasClient(texasConfig, createHttpClient())
    val appContext = ApplicationContext(
        consumer = consumer,
        dataSource = dataSource,
        prometheusMeterRegistry = prometheusMeterRegistry,
        securityConfig = securityConfig,
        healthChecks = healthChecksOf(
            consumer,
            DatasourceLivenessProbe(dataSource)
        ) + bakgrunnsprosesser.helthChecks(),
        idClient = webClients.kafkaClient,
        pdlClient = webClients.pdlClient,
        brukerprofilTjeneste = brukerprofilTjeneste,
        finnStillingerClient = webClients.finnStillingerClient,
        clock = clock,
        meterBinders = listOf(
            consumer.consumerMetrics,
            JvmGcMetrics(),
            JvmMemoryMetrics()
        ),
        texasClient = texasClient
    )
    runApp(appContext)
}

class ProcessorContext(
    val brukerprofilTjeneste: BrukerprofilTjeneste,
    val idFunction: (AktorId) -> Identitetsnummer?
)

fun ProcessorContext.process(source: Sequence<Message<Any, Any>>) {
    transaction {
        source
            .onEach { message ->
                appLogger.trace("Mottatt melding: topic=${message.topic} - partition=${message.partition} - offset=${message.offset}")
            }
            .forEach { message ->
                when (val value = message.value) {
                    is Profilering -> lagreProfilering(value)
                    is Periode -> opprettOgOppdaterBruker(value)
                    is Siste14aVedtakMelding -> lagre14aResultat(idFunction, brukerprofilTjeneste, value)
                }
            }
    }
}

