package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.DatabaseConfig
import org.flywaydb.core.Flyway
import org.postgresql.util.PGobject
import java.time.Duration
import javax.sql.DataSource

fun main() {
    val dataSource = generateDatasource("jdbc:postgresql://localhost:5432/arbeidssokerregisteretapisoek?user=admin&password=admin")
    cleanDatabase(dataSource)
}

fun migrateDatabase(dataSource: DataSource) {
    Flyway.configure().baselineOnMigrate(true).dataSource(dataSource).load().migrate()
}

fun cleanDatabase(dataSource: DataSource) {
    Flyway.configure().baselineOnMigrate(true).cleanDisabled(false).dataSource(dataSource).load().clean()
}

fun generateDatasource(url: String): DataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = url
            maximumPoolSize = 3
            connectionTimeout = Duration.ofSeconds(30).toMillis()
            maxLifetime = Duration.ofMinutes(30).toMillis()
        }
    )

fun DatabaseConfig.dataSource() =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = url
            driverClassName = "org.postgresql.Driver"
            password = password
            username = name
            isAutoCommit = false
        }
    )

class PGEnum<T : Enum<T>>(enumTypeName: String, enumValue: T?) : PGobject() {
    init {
        value = enumValue?.name
        type = enumTypeName
    }
}
