package no.naw.paw.minestillinger

import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.health.HealthChecks
import no.nav.paw.hwm.DataConsumer
import no.nav.paw.hwm.Message
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.pdl.client.PdlClient
import no.nav.paw.security.authentication.config.SecurityConfig
import no.nav.paw.security.texas.TexasClient
import no.naw.paw.minestillinger.brukerprofil.BrukerprofilTjeneste

data class ApplicationContext(
    val consumer: DataConsumer<Message<Any, Any>, Any, Any>,
    val dataSource: HikariDataSource,
    val prometheusMeterRegistry: PrometheusMeterRegistry,
    val securityConfig: SecurityConfig,
    val healthChecks: HealthChecks,
    val idClient: KafkaKeysClient,
    val pdlClient: PdlClient,
    val finnStillingerClient: FinnStillingerClient,
    val brukerprofilTjeneste: BrukerprofilTjeneste,
    val clock: Clock,
    val meterBinders: List<MeterBinder>,
    val texasClient: TexasClient
)

