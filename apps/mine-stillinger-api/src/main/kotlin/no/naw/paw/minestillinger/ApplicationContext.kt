package no.naw.paw.minestillinger

import com.zaxxer.hikari.HikariDataSource
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.health.HealthChecks
import no.nav.paw.hwm.DataConsumer
import no.nav.paw.hwm.Message
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.pdl.client.PdlClient
import no.nav.paw.security.authentication.config.SecurityConfig
import org.apache.avro.specific.SpecificRecord

data class ApplicationContext(
    val consumer: DataConsumer<Message<Long, SpecificRecord>, Long, SpecificRecord>,
    val dataSource: HikariDataSource,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val securityConfig: SecurityConfig,
    val healthChecks: HealthChecks,
    val idClient: KafkaKeysClient,
    val pdlClient: PdlClient,
    val brukerprofilTjeneste: BrukerprofilTjeneste
)

