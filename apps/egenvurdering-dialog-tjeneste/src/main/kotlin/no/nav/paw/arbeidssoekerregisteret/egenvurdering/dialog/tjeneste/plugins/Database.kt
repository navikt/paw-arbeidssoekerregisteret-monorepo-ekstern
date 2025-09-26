package no.nav.paw.arbeidssoekerregisteret.egenvurdering.dialog.tjeneste.plugins

import io.ktor.events.EventDefinition
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationPlugin
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.MonitoringEvent
import io.ktor.server.application.install
import io.ktor.server.application.log
import no.nav.paw.database.plugin.FlywayPlugin
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource

fun Application.configureDatabase(dataSource: DataSource) {
    //TODO: Fiks den ordentlige datasourcen i morgen
    install(DataSourcePluginMedNyExposed) {
        this.dataSource = dataSource
    }
    install(FlywayPlugin) {
        this.dataSource = dataSource
    }
}

val DataSourceReady: EventDefinition<Application> = EventDefinition()

class DataSourcePluginConfig {
    var dataSource: DataSource? = null

    companion object {
        const val PLUGIN_NAME = "DataSourcePlugin"
    }
}

val DataSourcePluginMedNyExposed: ApplicationPlugin<DataSourcePluginConfig> =
    createApplicationPlugin(DataSourcePluginConfig.PLUGIN_NAME, ::DataSourcePluginConfig) {
        application.log.info("Installerer {}", DataSourcePluginConfig.PLUGIN_NAME)
        val dataSource = requireNotNull(pluginConfig.dataSource) { "DataSource er null" }

        on(MonitoringEvent(ApplicationStarted)) { application ->
            application.log.info("Initializing data source")
            Database.connect(dataSource)
            application.monitor.raise(DataSourceReady, application)
        }

        on(MonitoringEvent(ApplicationStopping)) { application ->
            application.log.info("Closing data source")
            dataSource.connection.close()
        }
    }
