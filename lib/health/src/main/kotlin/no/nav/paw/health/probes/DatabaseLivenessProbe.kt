package no.nav.paw.health.probes

import no.nav.paw.health.liveness.LivenessCheck
import org.slf4j.LoggerFactory
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("db-health-probe")

class DatasourceLivenessProbe(private val dataSource: DataSource): LivenessCheck {
    override fun isAlive(): Boolean = runCatching {
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT 1").execute()
        }
    }.onFailure { error ->
        logger.warn("Databasen er ikke klar", error)
    }.getOrDefault(false)
}

fun databaseIsAliveCheck(dataSource: DataSource) = DatasourceLivenessProbe(dataSource)