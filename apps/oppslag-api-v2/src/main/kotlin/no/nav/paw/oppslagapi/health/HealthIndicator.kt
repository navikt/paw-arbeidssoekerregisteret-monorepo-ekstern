package no.nav.paw.oppslagapi.health

import io.ktor.http.HttpStatusCode
import no.nav.paw.logging.logger.buildApplicationLogger

private val logger = buildApplicationLogger

sealed interface HealthIndicator {
    val name: String
}

interface IsAlive : HealthIndicator {
    fun isAlive(): Status
}

interface IsReady : HealthIndicator {
    fun isReady(): Status
}

interface HasStarted : HealthIndicator {
    fun hasStarted(): Status
}

sealed interface NotOk {
    val message: String
}

sealed interface Status {
    data object OK : Status
    data class ERROR(override val message: String, val cause: Throwable? = null) : Status, NotOk
    data class PENDING(override val message: String) : Status, NotOk
}

fun Status.httpResponse(): Pair<HttpStatusCode, String> =
    when (this) {
        Status.OK -> HttpStatusCode.OK to "OK"
        is Status.ERROR -> HttpStatusCode.InternalServerError to message
        is Status.PENDING -> HttpStatusCode.ServiceUnavailable to message
    }.also { (code, message) ->
        if (code != HttpStatusCode.OK) {
            logger.warn("Status: '$code', Message: '$message'")
        }
    }
