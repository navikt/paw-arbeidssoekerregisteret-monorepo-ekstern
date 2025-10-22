package no.nav.paw.ledigestillinger.test

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.ledigestillinger.config.APPLICATION_CONFIG
import no.nav.paw.ledigestillinger.config.ApplicationConfig
import no.nav.paw.ledigestillinger.service.StillingService
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

class TestContext private constructor(
    val dataSource: DataSource,
    val applicationConfig: ApplicationConfig = loadNaisOrLocalConfiguration(APPLICATION_CONFIG),
    val meterRegistry: MeterRegistry = SimpleMeterRegistry(),
    val stillingService: StillingService = StillingService(applicationConfig, meterRegistry)
) {
    fun setUp(): TestContext {
        Database.connect(dataSource)
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .cleanDisabled(false)
            .load()
        flyway.clean()
        flyway.migrate()
        return this
    }

    fun tearDown(): TestContext {
        dataSource.connection.close()
        return this
    }

    companion object {
        fun buildWithDatabase(): TestContext {
            return TestContext(dataSource = buildPostgresDataSource())
        }
    }
}