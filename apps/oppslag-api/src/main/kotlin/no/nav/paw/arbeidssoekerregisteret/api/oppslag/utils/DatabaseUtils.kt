package no.nav.paw.arbeidssoekerregisteret.api.oppslag.utils

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.paw.arbeidssoekerregisteret.api.oppslag.config.DatabaseConfig
import org.postgresql.util.PGobject
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

class PGEnum<T : Enum<T>>(enumTypeName: String, enumValue: T?) : PGobject() {
    init {
        value = enumValue?.name
        type = enumTypeName
    }
}
