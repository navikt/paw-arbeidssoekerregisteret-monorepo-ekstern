package no.nav.paw.health.probes

import java.util.concurrent.atomic.AtomicBoolean

class KafkaConsumerLivenessProbe {
    private val isRunning = AtomicBoolean(false)

    fun markAlive() = isRunning.set(true)
    fun markUnhealthy() = isRunning.set(false)
    fun isRunning(): Boolean = isRunning.get()
}