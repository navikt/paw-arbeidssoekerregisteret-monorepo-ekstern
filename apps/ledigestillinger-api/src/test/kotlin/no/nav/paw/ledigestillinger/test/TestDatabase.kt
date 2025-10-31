package no.nav.paw.ledigestillinger.test

import no.nav.paw.database.config.DatabaseConfig
import no.nav.paw.database.factory.createHikariDataSource
import no.nav.paw.ledigestillinger.model.dao.ArbeidsgivereTable
import no.nav.paw.ledigestillinger.model.dao.EgenskaperTable
import no.nav.paw.ledigestillinger.model.dao.KategorierTable
import no.nav.paw.ledigestillinger.model.dao.KlassifiseringerTable
import no.nav.paw.ledigestillinger.model.dao.LokasjonerTable
import no.nav.paw.ledigestillinger.model.dao.StillingRow
import no.nav.paw.ledigestillinger.model.dao.StillingerTable
import no.nav.paw.ledigestillinger.model.dao.asStillingRow
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import javax.sql.DataSource

fun StillingerTable.selectRows(): List<StillingRow> = transaction {
    selectAll()
        .map {
            it.asStillingRow(
                arbeidsgiver = ArbeidsgivereTable::selectRowByParentId,
                kategorier = KategorierTable::selectRowsByParentId,
                klassifiseringer = KlassifiseringerTable::selectRowsByParentId,
                lokasjoner = LokasjonerTable::selectRowsByParentId,
                egenskaper = EgenskaperTable::selectRowsByParentId
            )
        }
}

fun buildPostgresDataSource(): DataSource {
    val config = postgreSQLContainer().let {
        DatabaseConfig(
            host = it.host,
            port = it.firstMappedPort,
            database = it.databaseName,
            username = it.username,
            password = it.password,
            autoCommit = false
        )
    }
    return createHikariDataSource(config)
}

private fun postgreSQLContainer(): PostgreSQLContainer<out PostgreSQLContainer<*>> {
    val postgres = PostgreSQLContainer(
        "postgres:17"
    ).apply {
        addEnv("POSTGRES_PASSWORD", "admin")
        addEnv("POSTGRES_USER", "admin")
        addEnv("POSTGRES_DATABASE", "ledigestillinger")
        addExposedPorts(5432)
    }
    postgres.start()
    postgres.waitingFor(Wait.forHealthcheck())
    return postgres
}
