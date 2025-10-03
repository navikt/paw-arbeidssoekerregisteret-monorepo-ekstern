package no.naw.paw.ledigestillinger

import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

data class HwmTopicConfig(
    val topic: String,
    val consumerVersion: Int,
    val partitionCount: Int
)

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
