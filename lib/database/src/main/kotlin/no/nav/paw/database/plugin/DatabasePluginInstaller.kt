package no.nav.paw.database.plugin

import io.ktor.server.application.Application
import io.ktor.server.application.install
import javax.sql.DataSource

fun Application.installDataSourcePlugin(
    dataSource: DataSource
) {
    install(DataSourcePlugin) {
        this.dataSource = dataSource
    }
}

fun Application.installFlywayPlugin(
    dataSource: DataSource,
    baselineOnMigrate: Boolean = true
) {
    install(FlywayPlugin) {
        this.dataSource = dataSource
        this.baselineOnMigrate = baselineOnMigrate
    }
}

fun Application.installCleanAwareFlywayPlugin(
    dataSource: DataSource,
    baselineOnMigrate: Boolean = true,
    cleanBeforeMigrate: Boolean = false // WARNING! Settes denne til true så tømmes databasen før migrering
) {
    install(CleanAwareFlywayPlugin) {
        this.dataSource = dataSource
        this.baselineOnMigrate = baselineOnMigrate
        this.cleanBeforeMigrate = cleanBeforeMigrate
    }
}

fun Application.installDatabasePlugins(
    dataSource: DataSource,
    baselineOnMigrate: Boolean = true
) {
    installDataSourcePlugin(
        dataSource = dataSource
    )
    installFlywayPlugin(
        dataSource = dataSource,
        baselineOnMigrate = baselineOnMigrate
    )
}

fun Application.installCleanAwareDatabasePlugins(
    dataSource: DataSource,
    baselineOnMigrate: Boolean = true,
    cleanBeforeMigrate: Boolean = false // WARNING! Settes denne til true så tømmes databasen før migrering
) {
    installDataSourcePlugin(
        dataSource = dataSource
    )
    installCleanAwareFlywayPlugin(
        dataSource = dataSource,
        baselineOnMigrate = baselineOnMigrate,
        cleanBeforeMigrate = cleanBeforeMigrate
    )
}
