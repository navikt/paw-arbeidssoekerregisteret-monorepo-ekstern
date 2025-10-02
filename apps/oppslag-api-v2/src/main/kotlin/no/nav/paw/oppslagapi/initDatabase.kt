package no.nav.paw.oppslagapi

import no.nav.paw.arbeidssokerregisteret.TopicNames
import no.nav.paw.arbeidssokerregisteret.asList
import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.oppslagapi.data.consumer.kafka.hwm.initHwm
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun initDatabase(topicNames: TopicNames, databaseConfig: DatabaseConfig) {
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
        topicNames.asList().forEach { topic ->
            initHwm(topic, consumer_version, partition_count)
        }
    }
}