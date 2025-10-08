package no.naw.paw.brukerprofiler

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.api.v1.Profilering
import no.nav.paw.arbeidssokerregisteret.api.v1.ProfilertTil
import no.nav.paw.arbeidssokerregisteret.standardTopicNames
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.health.healthChecksOf
import no.nav.paw.health.probes.DatasourceLivenessProbe
import no.nav.paw.hwm.HwmTopicConfig
import no.nav.paw.hwm.Message
import no.nav.paw.hwm.asMessageConsumerWithHwmAndMetrics
import no.nav.paw.kafka.config.KAFKA_CONFIG_WITH_SCHEME_REG
import no.nav.paw.kafka.factory.KafkaFactory
import no.nav.paw.kafkakeygenerator.client.createKafkaKeyGeneratorClient
import no.nav.paw.security.authentication.config.SECURITY_CONFIG
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.LongDeserializer
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.buildStatement
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import org.slf4j.LoggerFactory
import java.util.UUID

val appLogger = LoggerFactory.getLogger("brukerprofiler_api")

fun main() {
    appLogger.info("Starter brukerprofiler-api...")
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
        )
    )
    val kafkaFactory = KafkaFactory(loadNaisOrLocalConfiguration(KAFKA_CONFIG_WITH_SCHEME_REG))
    val consumer = kafkaFactory.createConsumer<Long, SpecificRecord>(
        groupId = "brukerprofiler_api_consumer_v1",
        clientId = "brukerprofiler_api_v1_${UUID.randomUUID()}",
        keyDeserializer = LongDeserializer::class,
        valueDeserializer = ActualSpecificAvroDeserializer::class
    ).asMessageConsumerWithHwmAndMetrics(
        prometheusMeterRegistry = prometheusMeterRegistry,
        receiver = ::process,
        hwmTopicConfig = topics
    )
    val dataSource = initDatabase(loadNaisOrLocalConfiguration(DATABASE_CONFIG))
    val idClient = createKafkaKeyGeneratorClient()
    val appContext = ApplicationContext(
        consumer = consumer,
        dataSource = dataSource,
        prometheusMeterRegistry = prometheusMeterRegistry,
        securityConfig = securityConfig,
        healthChecks = healthChecksOf(
            //consumer, Tar ut denne slik at appen kjører selv om vi aldre startet å konsumere meldinger
            DatasourceLivenessProbe(dataSource)
        ),
        idClient = idClient
    )
    runApp(appContext)
}

fun process(source: Sequence<Message<Long, SpecificRecord>>) {
    source.forEach { message ->
        when (val value = message.value) {
            is Profilering -> lagreProfilering(value)
            is Periode -> opprettOgOppdaterBruker(value)
        }
    }
}

fun lagreProfilering(profilering: Profilering) {
    ProfileringTable.upsert(
        keys = arrayOf(ProfileringTable.periodeId),
        onUpdateExclude = listOf(ProfileringTable.id, ProfileringTable.periodeId),
        body = {
            it[periodeId] = profilering.periodeId
            it[profileringId] = profilering.id
            it[profileringTidspunkt] = profilering.sendtInnAv.tidspunkt
            it[profileringResultat] = profilering.profilertTil.interntFormat().name
        }
    )
}

enum class ProfileringResultat {
    UKJENT_VERDI,
    UDEFINERT,
    ANTATT_GODE_MULIGHETER,
    ANTATT_BEHOV_FOR_VEILEDNING,
    OPPGITT_HINDRINGER
}

fun ProfilertTil.interntFormat(): ProfileringResultat = when (this) {
    ProfilertTil.UKJENT_VERDI -> ProfileringResultat.UKJENT_VERDI
    ProfilertTil.UDEFINERT -> ProfileringResultat.UDEFINERT
    ProfilertTil.ANTATT_GODE_MULIGHETER -> ProfileringResultat.ANTATT_GODE_MULIGHETER
    ProfilertTil.ANTATT_BEHOV_FOR_VEILEDNING -> ProfileringResultat.ANTATT_BEHOV_FOR_VEILEDNING
    ProfilertTil.OPPGITT_HINDRINGER -> ProfileringResultat.OPPGITT_HINDRINGER
}

fun opprettOgOppdaterBruker(periode: Periode) {
    val avsluttet = periode.avsluttet
    if (avsluttet != null) {
        BrukerTable.update {
            it[BrukerTable.arbeidssoekerperiodeAvsluttet] = avsluttet.tidspunkt
            it[BrukerTable.tjenestenErAktiv] = false
        }
    } else {
        BrukerTable.insertIgnore {
            it[identitetsnummer] = periode.identitetsnummer
            it[tjenestenErAktiv] = false
            it[harBruktTjenesten] = false
            it[arbeidssoekerperiodeId] = periode.id
            it[arbeidssoekerperiodeAvsluttet] = null
        }
    }
}

object BrukerTable : Table("periode") {
    val id = long("id").autoIncrement()
    val identitetsnummer = varchar("identitetsnummer", 11)
    val tjenestenErAktiv = bool("tjenesten_er_aktiv")
    val harBruktTjenesten = bool("har_brukt_tjenesten")
    val arbeidssoekerperiodeId = uuid("arbeidssoekerperiode_id")
    val arbeidssoekerperiodeAvsluttet = timestamp("arbeidssoekerperiode_avsluttet").nullable()

    override val primaryKey = PrimaryKey(id)
}

object ProfileringTable : Table("profilering") {
    val id = long("id").autoIncrement()
    val periodeId = uuid("periode_id")
    val profileringId = uuid("profilering_id")
    val profileringTidspunkt = timestamp("profilering_tidspunkt")
    val profileringResultat = varchar(name = "profilering_resultat", length = 255)

    override val primaryKey = PrimaryKey(id)
}