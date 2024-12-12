package no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins


import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.custom.DataSourcePlugin
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.plugins.custom.FlywayPlugin
import javax.sql.DataSource

fun Application.configureDatabase(dataSource: DataSource) {
    install(DataSourcePlugin) {
        this.dataSource = dataSource
    }
    install(FlywayPlugin) {
        this.dataSource = dataSource
    }
}
