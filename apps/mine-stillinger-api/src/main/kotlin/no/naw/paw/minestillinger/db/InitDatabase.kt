package no.naw.paw.minestillinger.db

import com.zaxxer.hikari.HikariDataSource
import no.nav.paw.config.env.ProdGcp
import no.nav.paw.config.env.currentRuntimeEnvironment
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database


fun initDatabase(databaseConfig: DatabaseConfig): HikariDataSource {
    val dataSource = createHikariDataSource(databaseConfig)
    Database.connect(dataSource)
    Flyway.configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .locations("db/migration")
        .load()
        .migrate()
    return dataSource
}
