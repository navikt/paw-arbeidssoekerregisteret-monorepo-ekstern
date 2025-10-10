package no.naw.paw.brukerprofiler.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.paw.config.env.Nais
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.RuntimeEnvironment
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database


fun initDatabase(databaseConfig: DatabaseConfig): HikariDataSource {
    val dataSource = createHikariDataSource(databaseConfig)
    val disableClean = false
    Database.connect(dataSource)
    if (currentRuntimeEnvironment is ProdGcp ) {
        if (!disableClean) throw IllegalStateException("Cannot run with clean enabled in ProdGcp")
    }
    Flyway.configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .locations("db/migration")
        .cleanDisabled(disableClean)
        .load()
        .also {
            it.clean()
            it.migrate()
        }
    return dataSource
}
