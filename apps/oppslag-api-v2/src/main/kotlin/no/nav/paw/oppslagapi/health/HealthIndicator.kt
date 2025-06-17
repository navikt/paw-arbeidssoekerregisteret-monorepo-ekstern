package no.nav.paw.oppslagapi.health

import io.ktor.http.HttpStatusCode
import no.nav.paw.oppslagapi.appLogger

fun interface IsAlive {
    operator fun invoke(): Status
}

fun interface IsReady {
    operator fun invoke(): Status
}

fun interface HasStarted {
    operator fun invoke(): Status
}

interface HealthIndicator {
    val isAlive: Status
    val isReady: Status
    val hasStarted: Status
}

sealed interface NotOk {
    val message: String
}
sealed interface Status {
    data object OK: Status
    data class ERROR(override val message: String): Status, NotOk
    data class PENDING(override val message: String): Status, NotOk
}

fun Status.httpResponse(): Pair<HttpStatusCode, String> =
    when (this) {
        Status.OK -> HttpStatusCode.OK to "OK"
        is Status.ERROR -> HttpStatusCode.InternalServerError to message
        is Status.PENDING -> HttpStatusCode.ServiceUnavailable to message
    }.also { (code, message) ->
        if (code != HttpStatusCode.OK) {
            appLogger.warn("Status: '$code', Message: '$message'")
        }
    }
