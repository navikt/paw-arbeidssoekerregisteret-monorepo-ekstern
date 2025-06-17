package no.nav.paw.oppslagapi.health

import org.jetbrains.exposed.sql.transactions.transaction

object ExposedIsAlive : IsAlive {
    override fun invoke(): Status =
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
}

object ExposedHasStarted : HasStarted {
    override fun invoke(): Status = ExposedIsAlive()
}

object ExposedIsReady : IsReady {
    override fun invoke(): Status = ExposedIsAlive()
}
