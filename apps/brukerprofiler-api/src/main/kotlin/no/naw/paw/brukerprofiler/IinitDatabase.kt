package no.naw.paw.brukerprofiler

import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.naw.paw.brukerprofiler.hwm.initHwm
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

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
    transaction {
        topics.forEach { (topic, consumerVersion, partitionCount) ->
            initHwm(
                topic = topic,
                consumerVersion = consumerVersion,
                partitionCount = partitionCount
            )
        }
    }
}
