package no.nav.paw.oppslagapi.plugin

import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.RouteScopedPlugin
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.hooks.CallFailed
import io.ktor.server.application.hooks.ResponseBodyReadyForSend
import io.ktor.server.application.isHandled
import io.ktor.server.logging.mdcProvider
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.uri
import io.ktor.server.routing.Route
import io.ktor.util.AttributeKey
import io.ktor.util.reflect.instanceOf
import no.nav.paw.error.handler.handleException
import no.nav.paw.error.model.ProblemDetails
import no.nav.paw.logging.logger.buildServerLogger
import kotlin.reflect.KClass

private val logger = buildServerLogger

fun Route.installErrorHandler(
    customResolver: (throwable: Throwable, request: ApplicationRequest) -> ProblemDetails? = { _, _ -> null }
) {
    install(RouteErrorHandler) {
        exception<Throwable> { call: ApplicationCall, cause: Throwable ->
            call.handleException(cause, customResolver)
        }
    }
}

val RouteErrorHandler: RouteScopedPlugin<RouteErrorHandlerConfig> = createRouteScopedPlugin(
    "RouteErrorHandler",
    ::RouteErrorHandlerConfig
) {
    val statusPageMarker = AttributeKey<Unit>("RouteErrorHandlerTriggered")
    val exceptions = HashMap(pluginConfig.exceptions)
    val statuses = HashMap(pluginConfig.statuses)
    val unhandled = pluginConfig.unhandled

    fun findHandlerByValue(cause: Throwable): io.ktor.server.plugins.statuspages.HandlerFunction? {
        val keys = exceptions.keys.filter { cause.instanceOf(it) }
        if (keys.isEmpty()) return null

        if (keys.size == 1) {
            return exceptions[keys.single()]
        }

        val key = selectNearestParentClass(cause, keys)
        return exceptions[key]
    }

    on(ResponseBodyReadyForSend) { call, content ->
        if (call.attributes.contains(statusPageMarker)) return@on

        val status = content.status ?: call.response.status()
        if (status == null) {
            logger.trace("No status code found for call: ${call.request.uri}")
            return@on
        }

        val handler = statuses[status]
        if (handler == null) {
            logger.trace("No handler found for status code {} for call: {}", status, call.request.uri)
            return@on
        }

        content.headers.entries().forEach { (key, values) ->
            values.forEach { value ->
                call.response.headers.append(key, value)
            }
        }

        call.attributes.put(statusPageMarker, Unit)
        try {
            logger.trace("Executing {} for status code {} for call: {}", handler, status, call.request.uri)
            handler(call, content, status)
        } catch (cause: Throwable) {
            logger.trace(
                "Exception {} while executing {} for status code {} for call: {}",
                cause,
                handler,
                status,
                call.request.uri
            )
            call.attributes.remove(statusPageMarker)
            throw cause
        }
    }

    on(CallFailed) { call, cause ->
        if (call.attributes.contains(statusPageMarker)) return@on

        logger.trace("Call ${call.request.uri} failed with cause $cause")

        val handler = findHandlerByValue(cause)
        if (handler == null) {
            logger.trace("No handler found for exception: {} for call {}", cause, call.request.uri)
            throw cause
        }

        call.attributes.put(statusPageMarker, Unit)
        call.application.mdcProvider.withMDCBlock(call) {
            logger.trace("Executing {} for exception {} for call {}", handler, cause, call.request.uri)
            handler(call, cause)
        }
    }

    on(BeforeFallback) { call ->
        if (call.isHandled) return@on
        unhandled(call)
    }
}

typealias HandlerFunction = suspend (call: ApplicationCall, cause: Throwable) -> Unit

class RouteErrorHandlerConfig {
    val exceptions: MutableMap<KClass<*>, HandlerFunction> = mutableMapOf()
    val statuses: MutableMap<HttpStatusCode,
            suspend (call: ApplicationCall, content: OutgoingContent, code: HttpStatusCode) -> Unit> = mutableMapOf()
    internal var unhandled: suspend (ApplicationCall) -> Unit = {}

    inline fun <reified T : Throwable> exception(
        noinline handler: suspend (call: ApplicationCall, cause: T) -> Unit
    ): Unit = exception(T::class, handler)

    fun <T : Throwable> exception(
        klass: KClass<T>,
        handler: suspend (call: ApplicationCall, T) -> Unit
    ) {
        @Suppress("UNCHECKED_CAST")
        val cast = handler as suspend (ApplicationCall, Throwable) -> Unit

        exceptions[klass] = cast
    }
}