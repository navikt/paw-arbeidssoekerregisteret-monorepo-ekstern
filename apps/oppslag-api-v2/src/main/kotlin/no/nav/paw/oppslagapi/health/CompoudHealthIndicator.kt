package no.nav.paw.oppslagapi.health

class CompoudHealthIndicator(
    vararg indicators: HealthIndicator
): IsAlive, IsReady, HasStarted {
    private val isAliveIndicators = indicators.filterIsInstance<IsAlive>()
    private val isReadyIndicators = indicators.filterIsInstance<IsReady>()
    private val hasStartedIndicators = indicators.filterIsInstance<HasStarted>()

    override val name: String = "CompoundHealthIndicator(${indicators.joinToString { it.name }})"

    override fun isAlive(): Status = isAliveIndicators
        .map(IsAlive::isAlive)
        .reduceToStatus()

    override fun isReady(): Status = isReadyIndicators
        .map(IsReady::isReady)
        .reduceToStatus()

    override fun hasStarted(): Status = hasStartedIndicators
        .map(HasStarted::hasStarted)
        .reduceToStatus()
}