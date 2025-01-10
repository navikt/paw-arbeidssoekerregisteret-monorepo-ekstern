package no.nav.paw.arbeidssoekerregisteret.eksternt.api.utils

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.paw.arbeidssoekerregisteret.eksternt.api.config.DatabaseConfig
import java.time.Duration
import javax.sql.DataSource

fun generateDatasource(databaseConfig: DatabaseConfig): DataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = databaseConfig.url
            maximumPoolSize = 3
            connectionTimeout = Duration.ofSeconds(30).toMillis()
            maxLifetime = Duration.ofMinutes(30).toMillis()
        }
    )
