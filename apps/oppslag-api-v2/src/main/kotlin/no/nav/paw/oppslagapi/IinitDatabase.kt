package no.nav.paw.oppslagapi

import no.nav.paw.arbeidssokerregisteret.TopicNames
import no.nav.paw.arbeidssokerregisteret.asList
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.database.config.DATABASE_CONFIG
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.oppslagapi.data.consumer.kafka.hwm.initHwm
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

fun initDatabase(topicNames: TopicNames) {
    val dataSource = createHikariDataSource(loadNaisOrLocalConfiguration(DATABASE_CONFIG))
    Database.Companion.connect(dataSource)
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