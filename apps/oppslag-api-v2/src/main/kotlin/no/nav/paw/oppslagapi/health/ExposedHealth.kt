package no.nav.paw.oppslagapi.health

import org.jetbrains.exposed.sql.transactions.transaction

object ExposedHealthIndicator : IsAlive, IsReady, HasStarted {
    override val name: String = "ExposedHealthIndicator"

    private fun checkDatabaseConnection(): Status =
        runCatching {
            transaction {
                exec("select 1") { rs ->
                    if (rs.next()) {
                        rs.getInt(1)
                        Status.OK
                    } else {
                        Status.ERROR("Exposed database connection failed")
                    }
                }
            } ?: Status.ERROR("Exposed database connection failed")
        }.recover { throwable ->
            Status.ERROR(
                message = "Exposed database connection failed: ${throwable.message ?: "Unknown error"}",
                cause = throwable
            )
        }.getOrThrow()

    override fun isAlive(): Status = checkDatabaseConnection()

    override fun isReady(): Status = checkDatabaseConnection()

    override fun hasStarted(): Status = checkDatabaseConnection()

}
