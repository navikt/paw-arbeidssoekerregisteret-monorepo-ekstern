package no.naw.paw.brukerprofiler

import com.zaxxer.hikari.HikariDataSource
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import javax.sql.DataSource


fun initDatabase(databaseConfig: DatabaseConfig): HikariDataSource {
    val dataSource = createHikariDataSource(databaseConfig)
    Database.connect(dataSource)
    Flyway.configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .locations("db/migration")
        .cleanDisabled(false)
        .load()
        .migrate()
    return dataSource
}
