package no.nav.paw.error.plugin

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.Hook
import io.ktor.util.pipeline.PipelinePhase
import kotlin.reflect.KClass

internal fun selectNearestParentClass(cause: Throwable, keys: List<KClass<*>>): KClass<*>? =
    keys.minByOrNull { distance(cause.javaClass, it.java) }

private fun distance(child: Class<*>, parent: Class<*>): Int {
    var result = 0
    var current = child
    while (current != parent) {
        current = current.superclass
        result++
    }

    return result
}

internal object BeforeFallback : Hook<suspend (ApplicationCall) -> Unit> {
    override fun install(pipeline: ApplicationCallPipeline, handler: suspend (ApplicationCall) -> Unit) {
        val phase = PipelinePhase("BeforeFallback")
        pipeline.insertPhaseBefore(ApplicationCallPipeline.Fallback, phase)
        pipeline.intercept(phase) { handler(context) }
    }
}