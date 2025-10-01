package no.nav.paw.health.probes

import no.nav.paw.health.liveness.LivenessCheck
import java.util.concurrent.atomic.AtomicBoolean

class KafkaConsumerLivenessProbe: LivenessCheck {
    private val isRunning = AtomicBoolean(false)

    fun markAlive() = isRunning.set(true)
    fun markUnhealthy() = isRunning.set(false)
    override fun isAlive(): Boolean = isRunning.get()
}