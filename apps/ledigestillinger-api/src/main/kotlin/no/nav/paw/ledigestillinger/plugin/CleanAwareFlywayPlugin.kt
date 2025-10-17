package no.nav.paw.ledigestillinger.plugin

import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.log
import no.nav.paw.database.plugin.DataSourceReady
import no.nav.paw.database.plugin.FlywayMigrationCompleted
import no.nav.paw.ledigestillinger.plugin.CleanAwareFlywayPluginConfig.Companion.PLUGIN_NAME
import org.flywaydb.core.Flyway
import javax.sql.DataSource

class CleanAwareFlywayPluginConfig {
    var dataSource: DataSource? = null
    var baselineOnMigrate: Boolean = true
    var cleanBeforeMigrate: Boolean = false

    companion object {
        const val PLUGIN_NAME = "CleanAwareFlywayPlugin"
    }
}

val CleanAwareFlywayPlugin: ApplicationPlugin<CleanAwareFlywayPluginConfig> =
    createApplicationPlugin(PLUGIN_NAME, ::CleanAwareFlywayPluginConfig) {
        application.log.info("Installerer {}", PLUGIN_NAME)
        val dataSource = requireNotNull(pluginConfig.dataSource) { "DataSource er null" }
        val baselineOnMigrate = pluginConfig.baselineOnMigrate
        val cleanBeforeMigrate = pluginConfig.cleanBeforeMigrate

        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(baselineOnMigrate)
            .cleanDisabled(!cleanBeforeMigrate)
            .load()

        on(MonitoringEvent(DataSourceReady)) { application ->
            application.log.info("Running database migration")
            if (cleanBeforeMigrate) {
                flyway.clean()
            }
            flyway.migrate()
            application.monitor.raise(FlywayMigrationCompleted, application)
        }
    }
