package no.nav.paw.database.factory

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.paw.database.config.DatabaseConfig

fun createHikariDataSource(
    databaseConfig: DatabaseConfig,
    useServerPreparedStatements: Boolean = true,
) = HikariDataSource(
    HikariConfig().apply {
        jdbcUrl = databaseConfig.buildJdbcUrl()
        maximumPoolSize = databaseConfig.maximumPoolSize
        isAutoCommit = databaseConfig.autoCommit
        connectionTimeout = databaseConfig.connectionTimeout.toMillis()
        idleTimeout = databaseConfig.idleTimeout.toMillis()
        maxLifetime = databaseConfig.maxLifetime.toMillis()

        if (!useServerPreparedStatements) addDataSourceProperty("prepareThreshold", "0")
    }
)
