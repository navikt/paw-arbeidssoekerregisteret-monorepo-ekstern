package no.nav.paw.arbeidssoekerregisteret.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.database.plugin.DataSourcePlugin
import no.nav.paw.database.plugin.FlywayPlugin
import javax.sql.DataSource

fun Application.configureDatabase(dataSource: DataSource) {
    install(DataSourcePlugin) {
        this.dataSource = dataSource
    }
    install(FlywayPlugin) {
        this.dataSource = dataSource
    }
}
