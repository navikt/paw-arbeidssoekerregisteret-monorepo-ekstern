package no.naw.paw.brukerprofiler

import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.hwm.HwmTopicConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction


fun initDatabase(topics: Iterable<HwmTopicConfig>, databaseConfig: DatabaseConfig) {
    val dataSource = createHikariDataSource(databaseConfig)
    Database.connect(dataSource)
    Flyway.configure()
        .dataSource(dataSource)
        .baselineOnMigrate(true)
        .locations("db/migration")
        .cleanDisabled(false)
        .load()
        .migrate()
}
