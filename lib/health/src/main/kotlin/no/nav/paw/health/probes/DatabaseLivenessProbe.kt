package no.nav.paw.health.probes

import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("db-health-probe")

fun isDatabaseReady(dataSource: DataSource): Boolean = runCatching {
    dataSource.connection.use { connection ->
        connection.prepareStatement("SELECT 1").execute()
    }
}.onFailure { error ->
    logger.warn("Databasen er ikke klar", error)
}.getOrDefault(false)