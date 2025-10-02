package no.nav.paw.health

sealed interface HealthCheck {
    val name: String get() = this::class.simpleName ?: "Unknown"
}

class HealthChecks(vararg healthChecks: HealthCheck) : LivenessCheck, ReadinessCheck, StartupCheck,
    Iterable<HealthCheck> {
    private val alive = healthChecks.filterIsInstance<LivenessCheck>()
    private val ready = healthChecks.filterIsInstance<ReadinessCheck>()
    private val started = healthChecks.filterIsInstance<StartupCheck>()

    override fun isAlive(): Boolean = alive.all { it.isAlive() }

    override fun isReady(): Boolean = ready.all { it.isReady() }

    override fun hasStarted(): Boolean = started.all { it.hasStarted() }

    override fun iterator(): Iterator<HealthCheck> = (alive + ready + started).iterator()

}

fun healthChecksOf(vararg healthChecks: HealthCheck) = HealthChecks(*healthChecks)
